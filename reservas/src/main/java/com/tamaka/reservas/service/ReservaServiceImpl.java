package com.tamaka.reservas.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tamaka.common.error.ResourceNotFoundException;
import com.tamaka.common.event.DomainEvent;
import com.tamaka.common.event.EventPublisher;
import com.tamaka.reservas.domain.EstadoReserva;
import com.tamaka.reservas.domain.HistorialEstadoReserva;
import com.tamaka.reservas.domain.SolicitudReserva;
import com.tamaka.reservas.dto.request.CancelarReservaRequest;
import com.tamaka.reservas.dto.request.CrearReservaRequest;
import com.tamaka.reservas.dto.request.ResponderSolicitudRequest;
import com.tamaka.reservas.dto.response.ReservaResponse;
import com.tamaka.reservas.exception.FechaReservaInvalidaException;
import com.tamaka.reservas.exception.NumeroPersonasFueraDeRangoException;
import com.tamaka.reservas.exception.ServicioNoDisponibleException;
import com.tamaka.reservas.exception.ValorAPagarRequeridoException;
import com.tamaka.reservas.mapper.ReservaMapper;
import com.tamaka.reservas.repository.HistorialEstadoReservaRepository;
import com.tamaka.reservas.repository.SolicitudReservaRepository;

@Service
@Transactional(readOnly = true)
public class ReservaServiceImpl implements ReservaService {

    private static final String AGREGADO = "Reserva";
    private static final String CANCELADA_POR_SISTEMA = "SISTEMA";

    private final SolicitudReservaRepository reservaRepository;
    private final HistorialEstadoReservaRepository historialRepository;
    private final ServicioCatalogo servicioCatalogo;
    private final ReservaMapper reservaMapper;
    private final EventPublisher eventPublisher;
    private final ReservasProperties properties;

    public ReservaServiceImpl(SolicitudReservaRepository reservaRepository,
                              HistorialEstadoReservaRepository historialRepository,
                              ServicioCatalogo servicioCatalogo,
                              ReservaMapper reservaMapper,
                              EventPublisher eventPublisher,
                              ReservasProperties properties) {
        this.reservaRepository = reservaRepository;
        this.historialRepository = historialRepository;
        this.servicioCatalogo = servicioCatalogo;
        this.reservaMapper = reservaMapper;
        this.eventPublisher = eventPublisher;
        this.properties = properties;
    }

    @Override
    @Transactional
    public ReservaResponse solicitar(Long turistaId, CrearReservaRequest request) {
        ServicioCatalogo.ServicioRef servicio = servicioCatalogo.obtener(request.servicioId());
        if (!servicio.activo()) {
            throw new ServicioNoDisponibleException(servicio.id());
        }
        LocalDate fecha = request.fechaReserva();
        if (fecha.isBefore(LocalDate.now())) {
            throw new FechaReservaInvalidaException(fecha);
        }
        int personas = request.numeroPersonas();
        if (personas < servicio.minPersonas() || personas > servicio.maxPersonas()) {
            throw new NumeroPersonasFueraDeRangoException(servicio.minPersonas(), servicio.maxPersonas());
        }

        Instant ahora = Instant.now();
        SolicitudReserva reserva = SolicitudReserva.crear(
                turistaId, servicio.id(), servicio.operadorId(), fecha, personas, ahora);
        SolicitudReserva guardada = reservaRepository.save(reserva);
        registrarHistorial(guardada, null, EstadoReserva.PENDIENTE, "Solicitud creada");

        eventPublisher.publicar(DomainEvent.de("ReservaSolicitada", guardada.getId(), Map.of(
                "turistaId", turistaId,
                "operadorId", servicio.operadorId(),
                "servicioId", servicio.id(),
                "fechaReserva", fecha.toString())));
        return reservaMapper.toResponse(guardada);
    }

    @Override
    public List<ReservaResponse> misReservas(Long turistaId) {
        return reservaRepository.findByTuristaIdOrderByCreadaEnDesc(turistaId).stream()
                .map(reservaMapper::toResponse)
                .toList();
    }

    @Override
    public ReservaResponse detalleComoTurista(Long turistaId, Long reservaId) {
        return reservaMapper.toResponse(obtenerDeTurista(turistaId, reservaId));
    }

    @Override
    @Transactional
    public ReservaResponse cancelarComoTurista(Long turistaId, Long reservaId) {
        SolicitudReserva reserva = obtenerDeTurista(turistaId, reservaId);
        EstadoReserva anterior = reserva.getEstado();
        reserva.cancelar();
        registrarHistorial(reserva, anterior, EstadoReserva.CANCELADA, "Cancelada por el turista");
        publicarCancelacion(reserva, "TURISTA", anterior);
        return reservaMapper.toResponse(reserva);
    }

    @Override
    public List<ReservaResponse> reservasDelOperador(Long operadorId) {
        return reservaRepository.findByOperadorIdOrderByCreadaEnDesc(operadorId).stream()
                .map(reservaMapper::toResponse)
                .toList();
    }

    @Override
    public ReservaResponse detalleComoOperador(Long operadorId, Long reservaId) {
        return reservaMapper.toResponse(obtenerDeOperador(operadorId, reservaId));
    }

    @Override
    @Transactional
    public ReservaResponse responder(Long operadorId, Long reservaId, ResponderSolicitudRequest request) {
        SolicitudReserva reserva = obtenerDeOperador(operadorId, reservaId);
        Instant ahora = Instant.now();
        EstadoReserva anterior = reserva.getEstado();

        if (Boolean.TRUE.equals(request.aceptada())) {
            if (request.valorAPagar() == null) {
                throw new ValorAPagarRequeridoException();
            }
            reserva.aceptar(request.valorAPagar(), request.observacion(), ahora, properties.plazoPago());
            registrarHistorial(reserva, anterior, EstadoReserva.ACEPTADA, "Aceptada por el operador");
            // Montos y fechas viajan como texto para no perder precision en JSON.
            eventPublisher.publicar(DomainEvent.de("ReservaAceptada", reservaId, Map.of(
                    "turistaId", reserva.getTuristaId(),
                    "operadorId", operadorId,
                    "servicioId", reserva.getServicioId(),
                    "valorAPagar", reserva.getValorAPagar().toPlainString(),
                    "plazoPago", reserva.getPlazoPago().toString())));
        } else {
            reserva.rechazar(request.observacion(), ahora);
            registrarHistorial(reserva, anterior, EstadoReserva.RECHAZADA, "Rechazada por el operador");
            eventPublisher.publicar(DomainEvent.de("ReservaRechazada", reservaId, Map.of(
                    "turistaId", reserva.getTuristaId(),
                    "operadorId", operadorId,
                    "servicioId", reserva.getServicioId())));
        }
        return reservaMapper.toResponse(reserva);
    }

    @Override
    @Transactional
    public ReservaResponse cancelarComoOperador(Long operadorId, Long reservaId, CancelarReservaRequest request) {
        SolicitudReserva reserva = obtenerDeOperador(operadorId, reservaId);
        EstadoReserva anterior = reserva.getEstado();
        reserva.cancelarPorOperador();
        String motivo = request == null || request.motivo() == null || request.motivo().isBlank()
                ? "Cancelada por el operador"
                : "Cancelada por el operador: " + request.motivo().trim();
        registrarHistorial(reserva, anterior, EstadoReserva.CANCELADA, motivo);
        publicarCancelacion(reserva, "OPERADOR", anterior);
        return reservaMapper.toResponse(reserva);
    }

    @Override
    public List<ReservaResponse> listarTodas(EstadoReserva estado) {
        List<SolicitudReserva> reservas = estado == null
                ? reservaRepository.findAllByOrderByCreadaEnDesc()
                : reservaRepository.findByEstadoOrderByCreadaEnDesc(estado);
        return reservas.stream().map(reservaMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public boolean confirmarPorPago(Long reservaId) {
        SolicitudReserva reserva = obtener(reservaId);
        if (reserva.pagoYaAplicado()) {
            // PagoAprobado repetido (reentrega del bus): no es un pago tardio, no se hace nada.
            return true;
        }
        EstadoReserva anterior = reserva.getEstado();
        if (reserva.confirmarPorPago()) {
            registrarHistorial(reserva, anterior, EstadoReserva.CONFIRMADA, "Pago aprobado");
            return true;
        }
        // Pago tardio sobre una solicitud no ACEPTADA: no se confirma y se pide revision de reembolso (RF-PAG-07).
        eventPublisher.publicar(DomainEvent.de("PagoRequiereRevision", reservaId, Map.of(
                "estadoReserva", anterior.name(),
                "turistaId", reserva.getTuristaId(),
                "operadorId", reserva.getOperadorId())));
        return false;
    }

    @Override
    @Transactional
    public int cancelarPorServicioDesactivado(Long servicioId) {
        int canceladas = 0;
        for (SolicitudReserva reserva : reservaRepository.findByServicioIdAndEstadoIn(
                servicioId, EnumSet.of(EstadoReserva.PENDIENTE, EstadoReserva.ACEPTADA))) {
            if (reserva.afectadaPorServicioInactivo()) {
                cancelarPorSistema(reserva, "El servicio fue desactivado");
                canceladas++;
            }
        }
        return canceladas;
    }

    @Override
    @Transactional
    public int cancelarVencidas(Instant ahora) {
        int canceladas = 0;
        for (SolicitudReserva reserva : reservaRepository.findByEstadoAndCreadaEnBefore(
                EstadoReserva.PENDIENTE, ahora.minus(properties.plazoRespuesta()))) {
            if (reserva.respuestaVencida(ahora, properties.plazoRespuesta())) {
                cancelarPorSistema(reserva, "El operador no respondio en el plazo");
                canceladas++;
            }
        }
        for (SolicitudReserva reserva : reservaRepository.findByEstadoAndPlazoPagoBefore(
                EstadoReserva.ACEPTADA, ahora)) {
            if (reserva.pagoVencido(ahora)) {
                cancelarPorSistema(reserva, "No se realizo el pago en el plazo");
                canceladas++;
            }
        }
        return canceladas;
    }

    @Override
    @Transactional
    public int finalizarConcluidas(LocalDate hoy) {
        int finalizadas = 0;
        for (SolicitudReserva reserva : reservaRepository.findByEstadoAndFechaReservaBefore(
                EstadoReserva.CONFIRMADA, hoy)) {
            if (reserva.concluidaAntesDe(hoy)) {
                reserva.finalizar();
                registrarHistorial(reserva, EstadoReserva.CONFIRMADA, EstadoReserva.FINALIZADA,
                        "La fecha del servicio ya paso");
                eventPublisher.publicar(DomainEvent.de("ReservaFinalizada", reserva.getId(), Map.of(
                        "turistaId", reserva.getTuristaId(),
                        "operadorId", reserva.getOperadorId(),
                        "servicioId", reserva.getServicioId())));
                finalizadas++;
            }
        }
        return finalizadas;
    }

    private void cancelarPorSistema(SolicitudReserva reserva, String motivo) {
        EstadoReserva anterior = reserva.getEstado();
        reserva.cancelar();
        registrarHistorial(reserva, anterior, EstadoReserva.CANCELADA, motivo);
        publicarCancelacion(reserva, CANCELADA_POR_SISTEMA, anterior);
    }

    private void publicarCancelacion(SolicitudReserva reserva, String canceladaPor, EstadoReserva anterior) {
        Map<String, Object> datos = new HashMap<>();
        datos.put("turistaId", reserva.getTuristaId());
        datos.put("operadorId", reserva.getOperadorId());
        datos.put("servicioId", reserva.getServicioId());
        datos.put("canceladaPor", canceladaPor);
        datos.put("estadoAnterior", anterior.name());
        eventPublisher.publicar(DomainEvent.de("ReservaCancelada", reserva.getId(), datos));
    }

    private void registrarHistorial(SolicitudReserva reserva, EstadoReserva anterior,
                                    EstadoReserva nuevo, String motivo) {
        historialRepository.save(new HistorialEstadoReserva(reserva.getId(), anterior, nuevo, motivo));
    }

    /** Regla de pertenencia del turista. */
    private SolicitudReserva obtenerDeTurista(Long turistaId, Long reservaId) {
        SolicitudReserva reserva = obtener(reservaId);
        if (!reserva.esDeTurista(turistaId)) {
            throw new AccessDeniedException("La reserva no pertenece al turista autenticado");
        }
        return reserva;
    }

    /** Regla de pertenencia del operador (dueno del servicio). */
    private SolicitudReserva obtenerDeOperador(Long operadorId, Long reservaId) {
        SolicitudReserva reserva = obtener(reservaId);
        if (!reserva.esDeOperador(operadorId)) {
            throw new AccessDeniedException("La solicitud no corresponde a un servicio del operador autenticado");
        }
        return reserva;
    }

    private SolicitudReserva obtener(Long reservaId) {
        return reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException(AGREGADO, reservaId));
    }
}
