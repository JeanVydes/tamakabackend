package com.tamaka.common.event.outbox;

/** Ciclo de vida de un mensaje del outbox. */
public enum EstadoOutbox {
    PENDIENTE,
    ENVIADO,
    /** Se agotaron los intentos o un suscriptor lo rechazo de forma definitiva: requiere revision manual. */
    FALLIDO
}
