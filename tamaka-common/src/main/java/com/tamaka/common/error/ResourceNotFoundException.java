package com.tamaka.common.error;

/**
 * Recurso inexistente. Se traduce a 404 Not Found.
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String recurso, Object id) {
        super(recurso + " no encontrado con id: " + id);
    }
}
