package com.tamaka.cuentas.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SuspensionRequest(

        @NotNull
        Boolean suspendida,

        @Size(max = 300)
        String motivo
) {
}
