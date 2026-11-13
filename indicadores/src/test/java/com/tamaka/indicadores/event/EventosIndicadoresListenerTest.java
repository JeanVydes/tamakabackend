package com.tamaka.indicadores.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tamaka.common.event.DomainEvent;
import com.tamaka.indicadores.service.IndicadoresService;

@ExtendWith(MockitoExtension.class)
class EventosIndicadoresListenerTest {

    private static final Instant OCURRIDO = Instant.parse("2026-11-09T10:00:00Z");

    @Mock
    private IndicadoresService indicadoresService;

    @InjectMocks
    private EventosIndicadoresListener listener;

    @Test
    void pagoAprobadoSeRegistraContraLaReserva() {
        listener.manejar(new DomainEvent("PagoAprobado", "99", OCURRIDO, Map.of("reservaId", 5, "operadorId", 7)));

        verify(indicadoresService).registrarHecho("PAGO_APROBADO", 7L, 5L, OCURRIDO);
    }

    @Test
    void reservaSolicitadaUsaElAgregadoComoReferencia() {
        listener.manejar(new DomainEvent("ReservaSolicitada", "5", OCURRIDO, Map.of("operadorId", 7)));

        verify(indicadoresService).registrarHecho("RESERVA_SOLICITADA", 7L, 5L, OCURRIDO);
    }

    @Test
    void reentregaConcurrenteQueChocaConElIndiceUnicoSeAceptaComoProcesada() {
        when(indicadoresService.registrarHecho("RESERVA_SOLICITADA", 7L, 5L, OCURRIDO))
                .thenThrow(new DataIntegrityViolationException("uk_registro_demanda"));

        assertThatCode(() -> listener.manejar(
                new DomainEvent("ReservaSolicitada", "5", OCURRIDO, Map.of("operadorId", 7))))
                .doesNotThrowAnyException();
    }
}
