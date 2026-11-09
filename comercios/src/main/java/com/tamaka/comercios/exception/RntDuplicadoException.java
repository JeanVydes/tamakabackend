package com.tamaka.comercios.exception;

import com.tamaka.common.error.BusinessException;

public class RntDuplicadoException extends BusinessException {

    public RntDuplicadoException() {
        super("Ya existe un comercio registrado con ese RNT");
    }
}
