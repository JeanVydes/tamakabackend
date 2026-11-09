package com.tamaka.reservas.exception;

import com.tamaka.common.error.BusinessException;

public class SolicitudYaRespondidaException extends BusinessException {

    public SolicitudYaRespondidaException(Long reservaId) {
        super("La solicitud " + reservaId + " ya fue respondida por el operador");
    }
}
