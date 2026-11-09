package com.tamaka.reservas.exception;

import com.tamaka.common.error.BusinessException;
import com.tamaka.reservas.domain.EstadoReserva;

public class TransicionReservaInvalidaException extends BusinessException {

    public TransicionReservaInvalidaException(Long reservaId, EstadoReserva desde, EstadoReserva hacia) {
        super("La solicitud " + reservaId + " no puede pasar de " + desde + " a " + hacia);
    }
}
