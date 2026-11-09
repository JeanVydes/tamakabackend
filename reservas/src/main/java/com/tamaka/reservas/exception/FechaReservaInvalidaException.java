package com.tamaka.reservas.exception;

import java.time.LocalDate;

import com.tamaka.common.error.BusinessException;

/** RF-RES-12: la fecha de la reserva no puede estar en el pasado. */
public class FechaReservaInvalidaException extends BusinessException {

    public FechaReservaInvalidaException(LocalDate fecha) {
        super("La fecha de reserva " + fecha + " no puede estar en el pasado");
    }
}
