package com.tamaka.preferencias.exception;

import com.tamaka.common.error.BusinessException;

public class FavoritoDuplicadoException extends BusinessException {

    public FavoritoDuplicadoException() {
        super("Este elemento ya esta en tus favoritos");
    }
}
