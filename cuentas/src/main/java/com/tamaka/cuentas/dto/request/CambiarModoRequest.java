package com.tamaka.cuentas.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CambiarModoRequest(

        @NotNull
        @Pattern(regexp = "TURISTA|OPERADOR", message = "debe ser TURISTA u OPERADOR")
        String modo
) {
}
