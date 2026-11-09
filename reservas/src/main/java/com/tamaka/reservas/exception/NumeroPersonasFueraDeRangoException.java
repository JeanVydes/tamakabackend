package com.tamaka.reservas.exception;

import com.tamaka.common.error.BusinessException;

/** RF-RES-12: el numero de personas debe estar dentro del minimo y maximo del servicio. */
public class NumeroPersonasFueraDeRangoException extends BusinessException {

    public NumeroPersonasFueraDeRangoException(int minimo, int maximo) {
        super("El numero de personas debe estar entre " + minimo + " y " + maximo);
    }
}
