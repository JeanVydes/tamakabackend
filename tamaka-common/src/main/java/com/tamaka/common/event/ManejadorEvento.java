package com.tamaka.common.event;

import java.util.Set;

/**
 * Puerto de entrada de eventos. Cada servicio implementa sus consumidores en su paquete {@code event}:
 * traducen el {@link DomainEvent} a una llamada de la capa de servicio y no contienen reglas de negocio.
 * El transporte (HTTP interno hoy, SQS manana) entrega los eventos a traves de {@link DespachadorEventos}.
 */
public interface ManejadorEvento {

    /** Tipos de evento que atiende este consumidor, p. ej. "ReservaAceptada". */
    Set<String> tiposSoportados();

    void manejar(DomainEvent evento);
}
