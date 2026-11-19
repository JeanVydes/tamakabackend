package com.tamaka.comercios.dto.response;

import java.util.List;

/**
 * Detalle publico de un comercio con sus servicios activos.
 */
public record ComercioDetalleResponse(
        Long id,
        Long operadorId,
        String nombre,
        String rnt,
        String categoria,
        String descripcion,
        boolean activo,
        List<ServicioResponse> servicios,
        /** URL publica de la portada (R2); null si no tiene. */
        String imagen
) {
}
