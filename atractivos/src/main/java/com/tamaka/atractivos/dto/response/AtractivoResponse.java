package com.tamaka.atractivos.dto.response;

public record AtractivoResponse(
        Long id,
        String nombre,
        String descripcion,
        String categoria,
        String estado,
        /** URL publica de la portada (R2); null si no tiene. */
        String imagen
) {
}
