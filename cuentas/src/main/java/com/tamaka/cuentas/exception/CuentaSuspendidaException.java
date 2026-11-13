package com.tamaka.cuentas.exception;

import com.tamaka.common.error.BusinessException;

/** Una cuenta suspendida no puede cambiar de modo ni recibir tokens nuevos. */
public class CuentaSuspendidaException extends BusinessException {

    public CuentaSuspendidaException(Long cuentaId) {
        super("La cuenta " + cuentaId + " esta suspendida");
    }
}
