package com.tamaka.pagos.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tamaka.common.error.ResourceNotFoundException;
import com.tamaka.common.event.DomainEvent;
import com.tamaka.common.event.EventPublisher;
import com.tamaka.pagos.domain.EstadoPago;
import com.tamaka.pagos.domain.Pago;
import com.tamaka.pagos.domain.ReservaPagable;
import com.tamaka.pagos.dto.request.IniciarPagoRequest;
import com.tamaka.pagos.dto.request.WebhookPasarelaRequest;
import com.tamaka.pagos.dto.response.ComprobanteResponse;
import com.tamaka.pagos.dto.response.PagoDetalleResponse;
import com.tamaka.pagos.dto.response.PagoResponse;
import com.tamaka.pagos.exception.MontoWebhookNoCoincideException;
import com.tamaka.pagos.exception.ReservaNoPagableException;
import com.tamaka.pagos.mapper.PagoMapper;
import com.tamaka.pagos.repository.PagoRepository;
import com.tamaka.pagos.repository.ReservaPagableRepository;

@Service
@Transactional(readOnly = true)
public class PagoServiceImpl implements PagoService {

    private static final String ESTADO_APROBADO = "APROBADO";

    private final PagoRepository pagoRepository;
    private final ReservaPagableRepository reservaPagableRepository;
    private final PasarelaPagos pasarela;
    private final PagoMapper pagoMapper;
    private final EventPublisher eventPublisher;
    private final PagosProperties properties;

    public PagoServiceImpl(PagoRepository pagoRepository,
                           ReservaPagableRepository reservaPagableRepository,
                           PasarelaPagos pasarela,
                           PagoMapper pagoMapper,
                           EventPublisher eventPublisher,
                           PagosProperties properties) {
        this.pagoRepository = pagoRepository;
        this.reservaPagableRepository = reservaPagableRepository;
        this.pasarela = pasarela;
        this.pagoMapper = pagoMapper;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
    }

    @Override
    @Transactional
    public PagoResponse iniciar(Long turistaId, IniciarPagoRequest request) {
        Long reservaId = request.reservaId();
        ReservaPagable reserva = reservaPagableRepository.findById(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva", reservaId));
        if (!reserva.getTuristaId().equals(turistaId)) {
            throw new AccessDeniedException("La reserva no pertenece al turista autenticado");
        }
        Instant ahora = Instant.now();
        if (!reserva.pagableEn(ahora)) {
            throw new ReservaNoPagableException(reservaId);
        }
        if (pagoRepository.findFirstByReservaIdAndEstado(reservaId, EstadoPago.APROBADO).isPresent()) {
            throw new ReservaNoPagableException(reservaId);
        }

        // Idempotente: si ya existe un intento pendiente, se devuelve el mismo.
        Optional<Pago> pendiente = pagoRepository.findFirstByReservaIdAndEstado(reservaId, EstadoPago.PENDIENTE);
        if (pendiente.isPresent()) {
            Pago existente = pendiente.get();
            return pagoMapper.toResponse(existente,
                    pasarela.crearCheckout(existente.getReferenciaPasarela(), existente.getMonto()));
        }

        String referencia = "TMK-" + UUID.randomUUID();
        Pago pago = pagoRepository.save(
                Pago.iniciar(reservaId, turistaId, reserva.getOperadorId(), reserva.getValorAPagar(), referencia, ahora));
        String urlPago = pasarela.crearCheckout(referencia, pago.getMonto());
        return pagoMapper.toResponse(pago, urlPago);
    }

    @Override
    public List<PagoResponse> misPagos(Long turistaId) {
        return pagoRepository.findByTuristaIdOrderByCreadoEnDesc(turistaId).stream()
                .map(pago -> pagoMapper.toResponse(pago, null))
                .toList();
    }

    @Override
    public ComprobanteResponse comprobante(Long turistaId, Long pagoId) {
        Pago pago = pagoRepository.findById(pagoId)
                .orElseThrow(() -> new ResourceNotFoundException("Pago", pagoId));
        if (!pago.esDeTurista(turistaId)) {
            throw new AccessDeniedException("El pago no pertenece al turista autenticado");
        }
        return pagoMapper.toComprobante(pago);
    }

    @Override
    @Transactional
    public void procesarWebhook(String firma, WebhookPasarelaRequest request) {
        String secreto = properties.webhookSecret();
        if (secreto.isBlank()) {
            throw new BadCredentialsException("Webhook no configurado");
        }
        if (!FirmaWebhook.coincide(FirmaWebhook.firmar(secreto, request), firma)) {
            throw new BadCredentialsException("Firma de webhook invalida");
        }
        Pago pago = pagoRepository.findByReferenciaPasarela(request.referencia())
                .orElseThrow(() -> new ResourceNotFoundException("Pago", request.referencia()));
        if (pago.getMonto().compareTo(request.monto()) != 0) {
            throw new MontoWebhookNoCoincideException(pago.getId());
        }

        if (ESTADO_APROBADO.equals(request.estado())) {
            // Idempotencia: un webhook repetido no vuelve a publicar el evento.
            if (pago.aprobar(Instant.now())) {
                pagoRepository.saveAndFlush(pago);
                eventPublisher.publicar(DomainEvent.de("PagoAprobado", pago.getId(), datosDelPago(pago)));
            }
        } else if (pago.getEstado() == EstadoPago.PENDIENTE) {
            pago.rechazar();
            pagoRepository.save(pago);
            eventPublisher.publicar(DomainEvent.de("PagoRechazado", pago.getId(), datosDelPago(pago)));
        }
    }

    @Override
    @Transactional
    public void registrarReservaAceptada(Long reservaId, Long turistaId, Long operadorId,
                                         BigDecimal valorAPagar, Instant plazoPago) {
        // Idempotente: una reentrega del evento no sobrescribe la proyeccion (podria estar ya cancelada).
        if (reservaPagableRepository.existsById(reservaId)) {
            return;
        }
        reservaPagableRepository.save(new ReservaPagable(reservaId, turistaId, operadorId, valorAPagar, plazoPago));
    }

    @Override
    @Transactional
    public void registrarReservaCancelada(Long reservaId) {
        reservaPagableRepository.findById(reservaId).ifPresent(ReservaPagable::cancelar);
        // Si ya estaba pagada, se reembolsa segun la politica (RF-PAG-07; politica completa pendiente).
        reembolsarAprobado(reservaId);
    }

    @Override
    @Transactional
    public void procesarRevisionPago(Long reservaId) {
        reembolsarAprobado(reservaId);
    }

    /** Reembolsa el pago aprobado de la reserva, si existe. Idempotente. */
    private void reembolsarAprobado(Long reservaId) {
        pagoRepository.findFirstByReservaIdAndEstado(reservaId, EstadoPago.APROBADO).ifPresent(pago -> {
            pasarela.reembolsar(pago.getReferenciaPasarela(), pago.getMonto());
            pago.reembolsar();
            pagoRepository.save(pago);
            eventPublisher.publicar(DomainEvent.de("ReembolsoRegistrado", pago.getId(), datosDelPago(pago)));
        });
    }

    @Override
    public List<PagoDetalleResponse> pagosDelOperador(Long operadorId) {
        return pagoRepository.findByOperadorIdOrderByCreadoEnDesc(operadorId).stream()
                .map(pagoMapper::toDetalle)
                .toList();
    }

    @Override
    public List<PagoDetalleResponse> listarTodos(EstadoPago estado) {
        List<Pago> pagos = estado == null
                ? pagoRepository.findAllByOrderByCreadoEnDesc()
                : pagoRepository.findByEstadoOrderByCreadoEnDesc(estado);
        return pagos.stream().map(pagoMapper::toDetalle).toList();
    }

    /** Payload comun de los eventos de pago: ids y monto como texto (sin datos de tarjeta, PCI DSS). */
    private static Map<String, Object> datosDelPago(Pago pago) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("reservaId", pago.getReservaId());
        datos.put("turistaId", pago.getTuristaId());
        if (pago.getOperadorId() != null) {
            datos.put("operadorId", pago.getOperadorId());
        }
        datos.put("monto", pago.getMonto().toPlainString());
        return datos;
    }
}
