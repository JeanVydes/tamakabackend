package com.tamaka.preferencias.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GuardarFavoritoRequest(

        @NotNull
        @Pattern(regexp = "ATRACTIVO|SERVICIO|EVENTO", message = "debe ser ATRACTIVO, SERVICIO o EVENTO")
        String tipo,

        @NotBlank
        @Size(max = 64)
        String refId
) {
}
