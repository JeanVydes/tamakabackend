package com.tamaka.comercios.service;

import org.springframework.web.multipart.MultipartFile;

import com.tamaka.comercios.dto.request.ActualizarServicioRequest;
import com.tamaka.comercios.dto.request.CambiarActivoRequest;
import com.tamaka.comercios.dto.response.ServicioResponse;

/**
 * Casos de uso sobre un servicio concreto.
 */
public interface ServicioService {

    ServicioResponse obtener(Long servicioId);

    ServicioResponse cambiarActivo(Long operadorId, Long servicioId, CambiarActivoRequest request);

    /** Actualizacion parcial por el operador dueno; valida el rango de personas resultante. */
    ServicioResponse actualizar(Long operadorId, Long servicioId, ActualizarServicioRequest request);

    /** Sube una portada JPEG/PNG/WebP (max 5 MB) para el servicio. */
    ServicioResponse subirImagen(Long operadorId, Long servicioId, MultipartFile archivo);

    /** Elimina la portada (y su objeto en el bucket). */
    ServicioResponse eliminarImagen(Long operadorId, Long servicioId);
}
