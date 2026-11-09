package com.tamaka.notificaciones.exception;

import com.tamaka.common.error.BusinessException;

public class TipoNotificacionDesconocidoException extends BusinessException {

    public TipoNotificacionDesconocidoException(String tipo) {
        super("No existe plantilla para el evento " + tipo);
    }
}
