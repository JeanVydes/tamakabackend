package com.tamaka.pagos.exception;

import com.tamaka.common.error.BusinessException;
import com.tamaka.pagos.domain.EstadoPago;

public class TransicionPagoInvalidaException extends BusinessException {

    public TransicionPagoInvalidaException(Long pagoId, EstadoPago desde, EstadoPago hacia) {
        super("El pago " + pagoId + " no puede pasar de " + desde + " a " + hacia);
    }
}
