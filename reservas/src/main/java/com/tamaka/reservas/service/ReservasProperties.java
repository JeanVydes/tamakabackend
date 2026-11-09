package com.tamaka.reservas.service;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Plazos parametrizables (el analisis detecto que RF-RES-14 y RF-PAG-06 deben usar un unico valor).
 *
 * @param plazoRespuesta tiempo maximo para que el operador responda (RF-RES-15)
 * @param plazoPago      tiempo maximo para pagar tras la aceptacion (RF-RES-14)
 */
@ConfigurationProperties(prefix = "tamaka.reservas")
public record ReservasProperties(Duration plazoRespuesta, Duration plazoPago) {

    public ReservasProperties {
        if (plazoRespuesta == null) {
            plazoRespuesta = Duration.ofMinutes(30);
        }
        if (plazoPago == null) {
            plazoPago = Duration.ofMinutes(60);
        }
    }
}
