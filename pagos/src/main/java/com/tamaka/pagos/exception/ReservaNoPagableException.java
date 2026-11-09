package com.tamaka.pagos.exception;

import com.tamaka.common.error.BusinessException;

/** La reserva no esta aceptada, esta cancelada o su plazo de pago vencio (RF-RES-14, RF-PAG-06). */
public class ReservaNoPagableException extends BusinessException {

    public ReservaNoPagableException(Long reservaId) {
        super("La reserva " + reservaId + " no se puede pagar en este momento");
    }
}
