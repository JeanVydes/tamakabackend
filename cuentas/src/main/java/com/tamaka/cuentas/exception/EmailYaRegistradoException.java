package com.tamaka.cuentas.exception;

import com.tamaka.common.error.BusinessException;

public class EmailYaRegistradoException extends BusinessException {

    public EmailYaRegistradoException(String email) {
        super("Ya existe una cuenta registrada con el correo indicado");
    }
}
