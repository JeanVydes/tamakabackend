package com.tamaka.common.event.outbox;

import com.tamaka.common.event.DomainEvent;
import com.tamaka.common.event.EventPublisher;

/**
 * Publicador transaccional: guarda el evento en el outbox dentro de la misma transaccion que el cambio de estado.
 * Si la transaccion se revierte, el evento tampoco existe; si el servicio cae tras el commit, el relay lo entrega
 * al volver. Los servicios siguen usando el puerto {@link EventPublisher} sin cambios.
 */
public class OutboxEventPublisher implements EventPublisher {

    private final OutboxStore store;

    public OutboxEventPublisher(OutboxStore store) {
        this.store = store;
    }

    @Override
    public void publicar(DomainEvent evento) {
        store.guardar(evento);
    }
}
