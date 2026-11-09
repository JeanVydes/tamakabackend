package com.tamaka.pagos.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record WebhookPasarelaRequest(

        @NotBlank
        @Size(max = 100)
        String referencia,

        @NotNull
        @Pattern(regexp = "APROBADO|RECHAZADO", message = "debe ser APROBADO o RECHAZADO")
        String estado,

        @NotNull
        @DecimalMin("0.00")
        BigDecimal monto
) {
}
