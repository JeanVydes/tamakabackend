package com.tamaka.pagos.exception;

import com.tamaka.common.error.BusinessException;

public class MontoWebhookNoCoincideException extends BusinessException {

    public MontoWebhookNoCoincideException(Long pagoId) {
        super("El monto notificado por la pasarela no coincide con el pago " + pagoId);
    }
}
