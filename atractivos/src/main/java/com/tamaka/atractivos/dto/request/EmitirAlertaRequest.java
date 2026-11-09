package com.tamaka.atractivos.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EmitirAlertaRequest(

        @NotNull
        Long atractivoId,

        @NotBlank
        @Size(max = 500)
        String mensaje
) {
}
