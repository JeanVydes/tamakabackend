package com.tamaka.comercios.dto.response;

public record ComercioResponse(
        Long id,
        Long operadorId,
        String nombre,
        String rnt,
        String categoria,
        String descripcion,
        boolean activo,
        /** URL publica de la portada (R2); null si no tiene. */
        String imagen
) {
}
