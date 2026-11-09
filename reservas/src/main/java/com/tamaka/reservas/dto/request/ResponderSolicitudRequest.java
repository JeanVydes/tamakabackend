package com.tamaka.reservas.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Decision del operador (RF-RES-08). valorAPagar es obligatorio al aceptar (regla de servicio).
 */
public record ResponderSolicitudRequest(

        @NotNull
        Boolean aceptada,

        @DecimalMin("0.00")
        BigDecimal valorAPagar,

        @Size(max = 500)
        String observacion
) {
}
