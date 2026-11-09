package com.tamaka.reservas.exception;

import com.tamaka.common.error.BusinessException;

public class ServicioNoDisponibleException extends BusinessException {

    public ServicioNoDisponibleException(Long servicioId) {
        super("El servicio " + servicioId + " no esta disponible para reservas");
    }
}
