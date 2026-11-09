package com.tamaka.cuentas.exception;

import com.tamaka.common.error.BusinessException;

public class CuentaYaSuspendidaException extends BusinessException {

    public CuentaYaSuspendidaException(Long cuentaId) {
        super("La cuenta " + cuentaId + " ya se encuentra suspendida");
    }
}
