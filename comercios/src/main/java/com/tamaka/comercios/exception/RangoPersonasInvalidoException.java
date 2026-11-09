package com.tamaka.comercios.exception;

import com.tamaka.common.error.BusinessException;

public class RangoPersonasInvalidoException extends BusinessException {

    public RangoPersonasInvalidoException(int minimo, int maximo) {
        super("El rango de personas es invalido: minimo " + minimo + " y maximo " + maximo);
    }
}
