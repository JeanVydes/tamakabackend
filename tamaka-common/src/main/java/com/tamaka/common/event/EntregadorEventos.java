package com.tamaka.common.event;

/**
 * Entrega sincrona de un evento a todos sus suscriptores. La usa el relay del outbox, que decide
 * reintentos y abandono; el transporte concreto (HTTP hoy, SNS manana) queda detras de esta interfaz.
 */
public interface EntregadorEventos {

    /** Resultado agregado de entregar un evento a todos sus suscriptores. */
    enum Resultado {
        /** Todos los suscriptores lo aceptaron (o el tipo no tiene suscriptores). */
        ENTREGADO,
        /** Algun suscriptor fallo de forma transitoria (red, 5xx): reintentar mas tarde. */
        REINTENTAR,
        /** Algun suscriptor lo rechazo de forma definitiva (4xx): reintentar no lo arreglara. */
        RECHAZADO
    }

    /** false si el transporte no puede entregar (p. ej., falta el secreto compartido). */
    boolean disponible();

    Resultado entregar(DomainEvent evento);
}
