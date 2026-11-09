package com.tamaka.pagos.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param webhookSecret secreto compartido con la pasarela para verificar la firma de los webhooks
 */
@ConfigurationProperties(prefix = "tamaka.pagos")
public record PagosProperties(String webhookSecret) {

    public PagosProperties {
        webhookSecret = webhookSecret == null ? "" : webhookSecret;
    }
}
