package com.tamaka.common.event.outbox;

import com.tamaka.common.event.DomainEvent;

import tools.jackson.databind.json.JsonMapper;

/**
 * Serializacion del evento que se guarda en el outbox: el mismo JSON que viaja por el bus.
 */
public final class EventoJson {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private EventoJson() {
    }

    public static String escribir(DomainEvent evento) {
        return MAPPER.writeValueAsString(evento);
    }

    public static DomainEvent leer(String json) {
        return MAPPER.readValue(json, DomainEvent.class);
    }
}
