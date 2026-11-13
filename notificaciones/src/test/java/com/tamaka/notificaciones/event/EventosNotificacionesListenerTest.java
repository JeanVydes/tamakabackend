package com.tamaka.notificaciones.event;

import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tamaka.common.event.DomainEvent;
import com.tamaka.notificaciones.service.NotificacionService;

@ExtendWith(MockitoExtension.class)
class EventosNotificacionesListenerTest {

    @Mock
    private NotificacionService notificacionService;

    @InjectMocks
    private EventosNotificacionesListener listener;

    @Test
    void pagoAprobadoDelegaConTuristaYOperador() {
        DomainEvent evento = DomainEvent.de("PagoAprobado", 99L, Map.of("reservaId", 5L, "turistaId", 1L, "operadorId", 7L));
        listener.manejar(evento);

        verify(notificacionService).notificarEvento(evento.id(), "PagoAprobado", 1L, 7L);
    }

    @Test
    void atractivoActualizadoAvisaASeguidores() {
        DomainEvent evento = DomainEvent.de("AtractivoActualizado", 4L, Map.of("estado", "CERRADO"));
        listener.manejar(evento);

        verify(notificacionService).notificarSeguidoresDeAtractivo(evento.id(), "AtractivoActualizado", 4L);
    }
}
