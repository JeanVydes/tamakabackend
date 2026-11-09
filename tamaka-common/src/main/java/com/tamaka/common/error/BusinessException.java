package com.tamaka.common.error;

/**
 * Base de las excepciones de negocio. Se traduce a 409 Conflict.
 * Cada regla de negocio debe tener su propia subclase con nombre propio (guia de servicios, sesion 9).
 */
public abstract class BusinessException extends RuntimeException {

    protected BusinessException(String message) {
        super(message);
    }
}
