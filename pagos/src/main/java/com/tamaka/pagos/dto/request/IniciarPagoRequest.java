package com.tamaka.pagos.dto.request;

import jakarta.validation.constraints.NotNull;

public record IniciarPagoRequest(

        @NotNull
        Long reservaId
) {
}
