package com.tamaka.atractivos.dto.response;

public record AtractivoResponse(
        Long id,
        String nombre,
        String descripcion,
        String categoria,
        String estado
) {
}
