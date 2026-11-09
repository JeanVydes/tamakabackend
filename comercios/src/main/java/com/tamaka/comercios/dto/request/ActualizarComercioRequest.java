package com.tamaka.comercios.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActualizarComercioRequest(

        @NotBlank
        @Size(max = 150)
        String nombre,

        @NotBlank
        @Size(max = 60)
        String categoria,

        @Size(max = 1000)
        String descripcion
) {
}
