package com.tamaka.reservas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.tamaka.common.event.DomainEvent;
import com.tamaka.common.event.EventPublisher;
import com.tamaka.reservas.domain.EstadoReserva;
import com.tamaka.reservas.domain.SolicitudReserva;
import com.tamaka.reservas.dto.request.CancelarReservaRequest;
import com.tamaka.reservas.dto.request.CrearReservaRequest;
import com.tamaka.reservas.dto.request.ResponderSolicitudRequest;
import com.tamaka.reservas.exception.NumeroPersonasFueraDeRangoException;
import com.tamaka.reservas.exception.SolicitudYaRespondidaException;
import com.tamaka.reservas.exception.TransicionReservaInvalidaException;
import com.tamaka.reservas.mapper.ReservaMapper;
import com.tamaka.reservas.repository.HistorialEstadoReservaRepository;
import com.tamaka.reservas.repository.SolicitudReservaRepository;

@ExtendWith(MockitoExtension.class)
class ReservaServiceImplTest {

    private static final Instant AHORA = Instant.parse("2026-11-09T10:00:00Z");

    @Mock
    private SolicitudReservaRepository reservaRepository;

    @Mock
    private HistorialEstadoReservaRepository historialRepository;

    @Mock
    private ServicioCatalogo servicioCatalogo;

    @Spy
    private ReservaMapper reservaMapper = new ReservaMapper();

    @Mock
    private EventPublisher eventPublisher;

    @Spy
    private ReservasProperties properties = new ReservasProperties(Duration.ofMinutes(30), Duration.ofMinutes(60));

    @InjectMocks
    private ReservaServiceImpl reservaService;

    @Test
    void rechazaNumeroDePersonasFueraDelRango() {
        when(servicioCatalogo.obtener(10L)).thenReturn(
                new ServicioCatalogo.ServicioRef(10L, 7L, 2, 4, true));

        assertThatThrownBy(() -> reservaService.solicitar(1L,
                new CrearReservaRequest(10L, LocalDate.now().plusDays(3), 6)))
                .isInstanceOf(NumeroPersonasFueraDeRangoException.class);

        verify(reservaRepository, never()).save(any());
    }

    @Test
    void operadorAjenoNoPuedeResponder() {
        SolicitudReserva reserva = SolicitudReserva.crear(1L, 10L, 7L, LocalDate.now().plusDays(2), 2, AHORA);
        when(reservaRepository.findById(5L)).thenReturn(Optional.of(reserva));

        assertThatThrownBy(() -> reservaService.responder(99L, 5L,
                new ResponderSolicitudRequest(true, new BigDecimal("80000"), null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void aceptarFijaValorYPlazoDePago() {
        SolicitudReserva reserva = SolicitudReserva.crear(1L, 10L, 7L, LocalDate.now().plusDays(2), 2, AHORA);
        when(reservaRepository.findById(5L)).thenReturn(Optional.of(reserva));

        var resultado = reservaService.responder(7L, 5L,
                new ResponderSolicitudRequest(true, new BigDecimal("80000"), "Incluye desayuno"));

        assertThat(resultado.estado()).isEqualTo("ACEPTADA");
        assertThat(resultado.valorAPagar()).isEqualByComparingTo("80000");
        assertThat(resultado.plazoPago()).isNotNull();
    }

    @Test
    void noPermiteResponderDosVeces() {
        SolicitudReserva reserva = SolicitudReserva.crear(1L, 10L, 7L, LocalDate.now().plusDays(2), 2, AHORA);
        reserva.rechazar("no disponible", AHORA);
        when(reservaRepository.findById(5L)).thenReturn(Optional.of(reserva));

        assertThatThrownBy(() -> reservaService.responder(7L, 5L,
                new ResponderSolicitudRequest(true, new BigDecimal("80000"), null)))
                .isInstanceOf(SolicitudYaRespondidaException.class);
    }

    @Test
    void pagoTardioSobreReservaCanceladaNoConfirma() {
        SolicitudReserva reserva = SolicitudReserva.crear(1L, 10L, 7L, LocalDate.now().plusDays(2), 2, AHORA);
        reserva.cancelar();
        when(reservaRepository.findById(5L)).thenReturn(Optional.of(reserva));

        boolean confirmada = reservaService.confirmarPorPago(5L);

        assertThat(confirmada).isFalse();
        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.CANCELADA);
    }

    @Test
    void cancelaSolicitudesVencidasSinTocarLasVigentes() {
        SolicitudReserva sinRespuesta = SolicitudReserva.crear(1L, 10L, 7L,
                LocalDate.now().plusDays(2), 2, AHORA.minus(Duration.ofMinutes(45)));
        when(reservaRepository.findByEstadoAndCreadaEnBefore(any(), any())).thenReturn(List.of(sinRespuesta));
        when(reservaRepository.findByEstadoAndPlazoPagoBefore(any(), any())).thenReturn(List.of());

        int canceladas = reservaService.cancelarVencidas(AHORA);

        assertThat(canceladas).isEqualTo(1);
        assertThat(sinRespuesta.getEstado()).isEqualTo(EstadoReserva.CANCELADA);
    }

    @Test
    void pagoAprobadoRepetidoEsIdempotente() {
        SolicitudReserva reserva = aceptada();
        when(reservaRepository.findById(5L)).thenReturn(Optional.of(reserva));

        assertThat(reservaService.confirmarPorPago(5L)).isTrue();
        assertThat(reservaService.confirmarPorPago(5L)).isTrue();

        assertThat(reserva.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
        verify(historialRepository, times(1)).save(any());
        verify(eventPublisher, never()).publicar(any());
    }

    @Test
    void servicioDesactivadoCancelaPendientesYAceptadas() {
        SolicitudReserva pendiente = SolicitudReserva.crear(1L, 10L, 7L, LocalDate.now().plusDays(2), 2, AHORA);
        SolicitudReserva aceptada = aceptada();
        when(reservaRepository.findByServicioIdAndEstadoIn(eq(10L), any()))
                .thenReturn(List.of(pendiente, aceptada));

        int canceladas = reservaService.cancelarPorServicioDesactivado(10L);

        assertThat(canceladas).isEqualTo(2);
        assertThat(pendiente.getEstado()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(aceptada.getEstado()).isEqualTo(EstadoReserva.CANCELADA);
        ArgumentCaptor<DomainEvent> eventos = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher, times(2)).publicar(eventos.capture());
        assertThat(eventos.getAllValues()).allSatisfy(e -> {
            assertThat(e.tipo()).isEqualTo("ReservaCancelada");
            assertThat(e.datoTexto("canceladaPor")).isEqualTo("SISTEMA");
        });
    }

    @Test
    void finalizaSoloConfirmadasCuyaFechaYaPaso() {
        SolicitudReserva pasada = SolicitudReserva.crear(1L, 10L, 7L, LocalDate.of(2026, 11, 1), 2, AHORA);
        pasada.aceptar(new BigDecimal("80000"), null, AHORA, Duration.ofMinutes(60));
        pasada.confirmarPorPago();
        SolicitudReserva deHoy = SolicitudReserva.crear(1L, 10L, 7L, LocalDate.of(2026, 11, 10), 2, AHORA);
        deHoy.aceptar(new BigDecimal("80000"), null, AHORA, Duration.ofMinutes(60));
        deHoy.confirmarPorPago();
        when(reservaRepository.findByEstadoAndFechaReservaBefore(EstadoReserva.CONFIRMADA, LocalDate.of(2026, 11, 10)))
                .thenReturn(List.of(pasada, deHoy));

        int finalizadas = reservaService.finalizarConcluidas(LocalDate.of(2026, 11, 10));

        assertThat(finalizadas).isEqualTo(1);
        assertThat(pasada.getEstado()).isEqualTo(EstadoReserva.FINALIZADA);
        assertThat(deHoy.getEstado()).isEqualTo(EstadoReserva.CONFIRMADA);
    }

    @Test
    void operadorCancelaReservaAceptadaYPublicaCancelacion() {
        SolicitudReserva reserva = aceptada();
        when(reservaRepository.findById(5L)).thenReturn(Optional.of(reserva));

        var resultado = reservaService.cancelarComoOperador(7L, 5L, new CancelarReservaRequest("Clima"));

        assertThat(resultado.estado()).isEqualTo("CANCELADA");
        ArgumentCaptor<DomainEvent> evento = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher).publicar(evento.capture());
        assertThat(evento.getValue().datoTexto("canceladaPor")).isEqualTo("OPERADOR");
    }

    @Test
    void operadorNoCancelaUnaSolicitudPendiente() {
        SolicitudReserva reserva = SolicitudReserva.crear(1L, 10L, 7L, LocalDate.now().plusDays(2), 2, AHORA);
        when(reservaRepository.findById(5L)).thenReturn(Optional.of(reserva));

        assertThatThrownBy(() -> reservaService.cancelarComoOperador(7L, 5L, null))
                .isInstanceOf(TransicionReservaInvalidaException.class);
        verify(eventPublisher, never()).publicar(any());
    }

    @Test
    void turistaNoVeDetalleDeReservaAjena() {
        SolicitudReserva reserva = SolicitudReserva.crear(1L, 10L, 7L, LocalDate.now().plusDays(2), 2, AHORA);
        when(reservaRepository.findById(5L)).thenReturn(Optional.of(reserva));

        assertThatThrownBy(() -> reservaService.detalleComoTurista(2L, 5L))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(reservaService.detalleComoTurista(1L, 5L).turistaId()).isEqualTo(1L);
    }

    @Test
    void aceptarPublicaMontoYPlazoComoTexto() {
        SolicitudReserva reserva = SolicitudReserva.crear(1L, 10L, 7L, LocalDate.now().plusDays(2), 2, AHORA);
        when(reservaRepository.findById(5L)).thenReturn(Optional.of(reserva));

        reservaService.responder(7L, 5L, new ResponderSolicitudRequest(true, new BigDecimal("80000.50"), null));

        ArgumentCaptor<DomainEvent> evento = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher).publicar(evento.capture());
        assertThat(evento.getValue().datos().get("valorAPagar")).isEqualTo("80000.50");
        assertThat(evento.getValue().datoInstanteRequerido("plazoPago")).isNotNull();
        assertThat(evento.getValue().datoLongRequerido("operadorId")).isEqualTo(7L);
    }

    private static SolicitudReserva aceptada() {
        SolicitudReserva reserva = SolicitudReserva.crear(1L, 10L, 7L, LocalDate.now().plusDays(2), 2, AHORA);
        reserva.aceptar(new BigDecimal("80000"), null, AHORA, Duration.ofMinutes(60));
        return reserva;
    }
}
