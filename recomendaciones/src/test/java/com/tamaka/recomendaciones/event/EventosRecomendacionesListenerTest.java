package com.tamaka.recomendaciones.event;

import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tamaka.common.event.DomainEvent;
import com.tamaka.recomendaciones.service.RecomendacionService;

@ExtendWith(MockitoExtension.class)
class EventosRecomendacionesListenerTest {

    @Mock
    private RecomendacionService recomendacionService;

    @InjectMocks
    private EventosRecomendacionesListener listener;

    @Test
    void servicioCreadoSeRegistraConLaCategoriaDelComercio() {
        listener.manejar(DomainEvent.de("ServicioCreado", 12L, Map.of("comercioId", 3L, "categoria", "HOSPEDAJE")));

        verify(recomendacionService).registrarItem("SERVICIO", 12L, "HOSPEDAJE");
    }

    @Test
    void servicioActualizadoReRegistraElServicio() {
        listener.manejar(DomainEvent.de("ServicioActualizado", 12L, Map.of("categoria", "TOURS")));

        verify(recomendacionService).registrarItem("SERVICIO", 12L, "TOURS");
    }

    @Test
    void servicioDesactivadoLoRetira() {
        listener.manejar(DomainEvent.de("ServicioDesactivado", 12L, Map.of("comercioId", 3L)));

        verify(recomendacionService).retirarItem("SERVICIO", 12L);
    }
}
