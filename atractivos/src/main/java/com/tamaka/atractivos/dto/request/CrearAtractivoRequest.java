package com.tamaka.atractivos.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrearAtractivoRequest(

        @NotBlank
        @Size(max = 150)
        String nombre,

        @Size(max = 2000)
        String descripcion,

        @NotBlank
        @Size(max = 60)
        String categoria
) {
}
