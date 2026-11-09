package com.tamaka.eventos.exception;

import java.time.LocalDate;

import com.tamaka.common.error.BusinessException;

public class RangoFechasInvalidoException extends BusinessException {

    public RangoFechasInvalidoException(LocalDate inicio, LocalDate fin) {
        super("La fecha de fin (" + fin + ") no puede ser anterior a la de inicio (" + inicio + ")");
    }
}
