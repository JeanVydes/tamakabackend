package com.tamaka.reservas.service;

import java.time.Instant;
import java.time.LocalDate;
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
                "servicioId", servicio.id())));
        return reservaMapper.toResponse(guardada);
    }

    @Override
    public List<ReservaResponse> misReservas(Long turistaId) {
        return reservaRepository.findByTuristaIdOrderByCreadaEnDesc(turistaId).stream()
                .map(reservaMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ReservaResponse cancelarComoTurista(Long turistaId, Long reservaId) {
        SolicitudReserva reserva = obtener(reservaId);
        if (!reserva.esDeTurista(turistaId)) {
            throw new AccessDeniedException("La reserva no pertenece al turista autenticado");
        }
        EstadoReserva anterior = reserva.getEstado();
        reserva.cancelar();
        registrarHistorial(reserva, anterior, EstadoReserva.CANCELADA, "Cancelada por el turista");
        publicarCancelacion(reserva, "TURISTA");
        return reservaMapper.toResponse(reserva);
    }

    @Override
    public List<ReservaResponse> reservasDelOperador(Long operadorId) {
        return reservaRepository.findByOperadorIdOrderByCreadaEnDesc(operadorId).stream()
                .map(reservaMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ReservaResponse responder(Long operadorId, Long reservaId, ResponderSolicitudRequest request) {
        SolicitudReserva reserva = obtener(reservaId);
        if (!reserva.esDeOperador(operadorId)) {
            throw new AccessDeniedException("La solicitud no corresponde a un servicio del operador autenticado");
        }
        Instant ahora = Instant.now();
        EstadoReserva anterior = reserva.getEstado();

        if (Boolean.TRUE.equals(request.aceptada())) {
            if (request.valorAPagar() == null) {
                throw new ValorAPagarRequeridoException();
            }
            reserva.aceptar(request.valorAPagar(), request.observacion(), ahora, properties.plazoPago());
            registrarHistorial(reserva, anterior, EstadoReserva.ACEPTADA, "Aceptada por el operador");
            eventPublisher.publicar(DomainEvent.de("ReservaAceptada", reservaId, Map.of(
                    "turistaId", reserva.getTuristaId(),
                    "operadorId", operadorId,
                    "valorAPagar", reserva.getValorAPagar(),
                    "plazoPago", reserva.getPlazoPago())));
        } else {
            reserva.rechazar(request.observacion(), ahora);
            registrarHistorial(reserva, anterior, EstadoReserva.RECHAZADA, "Rechazada por el operador");
            eventPublisher.publicar(DomainEvent.de("ReservaRechazada", reservaId, Map.of(
                    "turistaId", reserva.getTuristaId(),
                    "operadorId", operadorId)));
        }
        return reservaMapper.toResponse(reserva);
    }

    @Override
    @Transactional
    public boolean confirmarPorPago(Long reservaId) {
        SolicitudReserva reserva = obtener(reservaId);
        EstadoReserva anterior = reserva.getEstado();
        if (reserva.confirmarPorPago()) {
            registrarHistorial(reserva, anterior, EstadoReserva.CONFIRMADA, "Pago aprobado");
            return true;
        }
        // Pago tardio sobre una solicitud no ACEPTADA: no se confirma y se pide revision de reembolso (RF-PAG-07).
        eventPublisher.publicar(DomainEvent.de("PagoRequiereRevision", reservaId, Map.of(
                "estadoReserva", anterior.name())));
        return false;
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

    private void cancelarPorSistema(SolicitudReserva reserva, String motivo) {
        EstadoReserva anterior = reserva.getEstado();
        reserva.cancelar();
        registrarHistorial(reserva, anterior, EstadoReserva.CANCELADA, motivo);
        publicarCancelacion(reserva, "SISTEMA");
    }

    private void publicarCancelacion(SolicitudReserva reserva, String canceladaPor) {
        eventPublisher.publicar(DomainEvent.de("ReservaCancelada", reserva.getId(), Map.of(
                "turistaId", reserva.getTuristaId(),
                "operadorId", reserva.getOperadorId(),
                "canceladaPor", canceladaPor)));
    }

    private void registrarHistorial(SolicitudReserva reserva, EstadoReserva anterior,
                                    EstadoReserva nuevo, String motivo) {
        historialRepository.save(new HistorialEstadoReserva(reserva.getId(), anterior, nuevo, motivo));
    }

    private SolicitudReserva obtener(Long reservaId) {
        return reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ResourceNotFoundException(AGREGADO, reservaId));
    }
}
