package com.tamaka.comercios.dto.response;

public record ComercioResponse(
        Long id,
        Long operadorId,
        String nombre,
        String rnt,
        String categoria,
        String descripcion,
        boolean activo
) {
}
