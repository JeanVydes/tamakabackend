package com.tamaka.indicadores.exception;

import java.time.LocalDate;

import com.tamaka.common.error.BusinessException;

public class RangoFechasInvalidoException extends BusinessException {

    public RangoFechasInvalidoException(LocalDate desde, LocalDate hasta) {
        super("El rango de fechas es invalido: desde " + desde + " hasta " + hasta);
    }
}
