package com.tamaka.comercios.dto.request;

import java.math.BigDecimal;
import java.util.Map;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CrearServicioRequest(

        @NotBlank
        @Size(max = 150)
        String nombre,

        @NotNull
        @DecimalMin("0.00")
        BigDecimal precioBase,

        @NotNull
        @Min(1)
        Integer minPersonas,

        @NotNull
        @Min(1)
        Integer maxPersonas,

        Map<String, Object> atributos
) {
}
