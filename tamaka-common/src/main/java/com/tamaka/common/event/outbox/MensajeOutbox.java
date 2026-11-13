package com.tamaka.common.event.outbox;

import java.time.Instant;

import com.tamaka.common.event.DomainEvent;

/**
 * Mensaje reclamado del outbox.
 *
 * @param id       id del evento (clave del mensaje)
 * @param evento   evento a entregar
 * @param intentos intentos fallidos previos
 * @param creadoEn instante en que se guardo (orden de entrega)
 */
public record MensajeOutbox(String id, DomainEvent evento, int intentos, Instant creadoEn) {
}
