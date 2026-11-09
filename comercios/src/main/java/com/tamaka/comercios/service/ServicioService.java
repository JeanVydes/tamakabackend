package com.tamaka.comercios.service;

import com.tamaka.comercios.dto.request.CambiarActivoRequest;
import com.tamaka.comercios.dto.response.ServicioResponse;

/**
 * Casos de uso sobre un servicio concreto.
 */
public interface ServicioService {

    ServicioResponse obtener(Long servicioId);

    ServicioResponse cambiarActivo(Long operadorId, Long servicioId, CambiarActivoRequest request);
}
