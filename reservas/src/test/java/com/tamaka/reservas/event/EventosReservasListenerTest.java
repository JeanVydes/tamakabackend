package com.tamaka.reservas.event;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tamaka.common.event.DomainEvent;
import com.tamaka.common.event.EventoInvalidoException;
import com.tamaka.reservas.service.ReservaService;

@ExtendWith(MockitoExtension.class)
class EventosReservasListenerTest {

    @Mock
    private ReservaService reservaService;

    @InjectMocks
    private EventosReservasListener listener;

    @Test
    void pagoAprobadoConfirmaLaReservaDelPayload() {
        // El agregado de PagoAprobado es el pago; la reserva viaja en los datos (como Integer tras JSON).
        listener.manejar(new DomainEvent("PagoAprobado", "99", Instant.now(), Map.of("reservaId", 5)));

        verify(reservaService).confirmarPorPago(5L);
    }

    @Test
    void servicioDesactivadoCancelaPorServicio() {
        listener.manejar(DomainEvent.de("ServicioDesactivado", 10L, Map.of("comercioId", 3L)));

        verify(reservaService).cancelarPorServicioDesactivado(10L);
    }

    @Test
    void pagoAprobadoSinReservaEsInvalido() {
        assertThatThrownBy(() -> listener.manejar(DomainEvent.de("PagoAprobado", 99L, Map.of())))
                .isInstanceOf(EventoInvalidoException.class);
        verify(reservaService, never()).confirmarPorPago(anyLong());
    }
}
