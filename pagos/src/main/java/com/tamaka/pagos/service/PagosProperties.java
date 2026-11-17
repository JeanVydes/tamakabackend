package com.tamaka.pagos.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param webhookSecret secreto compartido con la pasarela para verificar la firma de los webhooks
 * @param aprobacionAutomatica mock de desarrollo/demo: al iniciar el pago se aprueba al instante
 *        (publica PagoAprobado y la reserva pasa a CONFIRMADA). Sin urlPago ni webhook.
 *        Por defecto true en los perfiles locales (ver application.yml): es la pasarela sandbox.
 *        Poner en false (TAMAKA_PAGOS_APROBACION_AUTOMATICA=false) al integrar una pasarela real,
 *        que confirmara de verdad por webhook.
 */
@ConfigurationProperties(prefix = "tamaka.pagos")
public record PagosProperties(String webhookSecret, Boolean aprobacionAutomatica) {

    public PagosProperties {
        webhookSecret = webhookSecret == null ? "" : webhookSecret;
        aprobacionAutomatica = aprobacionAutomatica == null ? Boolean.TRUE : aprobacionAutomatica;
    }
}
