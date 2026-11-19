package com.tamaka.common.storage;

import com.tamaka.common.error.BusinessException;

/** El bucket (Cloudflare R2) no esta configurado en este entorno: el servicio de medios esta deshabilitado. */
public class StorageNotAvailableException extends BusinessException {

    public StorageNotAvailableException() {
        super("El servicio de imagenes no esta configurado en este entorno");
    }
}
