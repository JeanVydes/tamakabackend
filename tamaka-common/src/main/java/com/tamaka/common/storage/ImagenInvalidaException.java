package com.tamaka.common.storage;

import java.util.LinkedHashMap;
import java.util.Map;

import com.tamaka.common.error.BusinessException;

/** El tipo MIME no esta admitido o el contenido no coincide con el tipo declarado. */
public class ImagenInvalidaException extends BusinessException {

    private final Map<String, String> detalles;

    public ImagenInvalidaException(String campo, String mensaje) {
        super(mensaje);
        Map<String, String> d = new LinkedHashMap<>();
        d.put(campo, mensaje);
        this.detalles = d;
    }

    public Map<String, String> getDetalles() {
        return Map.copyOf(detalles);
    }
}
