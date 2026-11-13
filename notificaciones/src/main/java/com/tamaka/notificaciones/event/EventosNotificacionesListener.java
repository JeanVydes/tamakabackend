package com.tamaka.notificaciones.event;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.tamaka.common.event.DomainEvent;
import com.tamaka.common.event.ManejadorEvento;
import com.tamaka.notificaciones.service.NotificacionService;

/**
 * Consumidor de los eventos que generan avisos (arquitectura sec. 5): reservas, pagos, alertas y atractivos.
 * Solo traduce el evento y delega; quien recibe cada aviso lo decide la capa de servicio.
 */
@Component
public class EventosNotificacionesListener implements ManejadorEvento {

    static final Set<String> EVENTOS_DE_RESERVA = Set.of(
            "ReservaSolicitada", "ReservaAceptada", "ReservaRechazada", "ReservaCancelada",
            "PagoAprobado", "PagoRechazado");
    static final Set<String> EVENTOS_DE_ATRACTIVO = Set.of("AtractivoActualizado", "AlertaSeguridadEmitida");

    private final NotificacionService notificacionService;

    public EventosNotificacionesListener(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @Override
    public Set<String> tiposSoportados() {
        return Set.of("ReservaSolicitada", "ReservaAceptada", "ReservaRechazada", "ReservaCancelada",
                "PagoAprobado", "PagoRechazado", "AtractivoActualizado", "AlertaSeguridadEmitida");
    }

    @Override
    public void manejar(DomainEvent evento) {
        if (EVENTOS_DE_RESERVA.contains(evento.tipo())) {
            notificacionService.notificarEvento(evento.id(), evento.tipo(),
                    evento.datoLong("turistaId"), evento.datoLong("operadorId"));
        } else if (EVENTOS_DE_ATRACTIVO.contains(evento.tipo())) {
            notificacionService.notificarSeguidoresDeAtractivo(evento.id(), evento.tipo(),
                    evento.agregadoIdLong());
        }
    }
}
