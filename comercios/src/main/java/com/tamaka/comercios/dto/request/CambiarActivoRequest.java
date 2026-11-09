package com.tamaka.comercios.dto.request;

import jakarta.validation.constraints.NotNull;

public record CambiarActivoRequest(

        @NotNull
        Boolean activo
) {
}
