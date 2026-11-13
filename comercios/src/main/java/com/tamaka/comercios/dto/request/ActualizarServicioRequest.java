package com.tamaka.comercios.dto.request;

import java.math.BigDecimal;
import java.util.Map;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Actualizacion parcial (PATCH) de un servicio: los campos nulos no se modifican.
 */
public record ActualizarServicioRequest(

        @Size(min = 1, max = 150)
        String nombre,

        @DecimalMin("0.00")
        BigDecimal precioBase,

        @Min(1)
        Integer minPersonas,

        @Min(1)
        Integer maxPersonas,

        Map<String, Object> atributos
) {
}
