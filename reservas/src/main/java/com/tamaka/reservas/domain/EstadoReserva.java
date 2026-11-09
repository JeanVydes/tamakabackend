package com.tamaka.reservas.domain;

/**
 * Maquina de estados unica de la solicitud (SRS 3.5). "Aceptada", "pagada" y "completada" se derivan de este estado.
 */
public enum EstadoReserva {
    PENDIENTE,
    ACEPTADA,
    RECHAZADA,
    CANCELADA,
    CONFIRMADA,
    FINALIZADA
}
