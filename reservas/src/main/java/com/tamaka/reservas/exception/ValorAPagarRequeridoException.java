package com.tamaka.reservas.exception;

import com.tamaka.common.error.BusinessException;

/** RF-RES-08: al aceptar, el operador debe indicar el valor a pagar. */
public class ValorAPagarRequeridoException extends BusinessException {

    public ValorAPagarRequeridoException() {
        super("Para aceptar la solicitud debe indicar el valor a pagar");
    }
}
