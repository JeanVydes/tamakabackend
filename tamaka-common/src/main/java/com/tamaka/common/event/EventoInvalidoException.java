package com.tamaka.common.event;

import com.tamaka.common.error.BusinessException;

/**
 * El evento recibido no trae un dato obligatorio o su formato no es valido. Se traduce a 409:
 * reintentar el mismo mensaje no lo arreglara.
 */
public class EventoInvalidoException extends BusinessException {

    public EventoInvalidoException(String tipo, String clave) {
        super("El evento " + tipo + " no trae un valor valido para '" + clave + "'");
    }
}
