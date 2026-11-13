package com.tamaka.cuentas.exception;

import com.tamaka.common.error.BusinessException;

/** Se intento pasar a modo operador sin haber habilitado antes el rol OPERADOR. */
public class ModoOperadorNoHabilitadoException extends BusinessException {

    public ModoOperadorNoHabilitadoException(Long cuentaId) {
        super("La cuenta " + cuentaId + " no tiene habilitado el modo operador");
    }
}
