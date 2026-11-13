package com.tamaka.cuentas.service;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param refreshTokenTtl vida del refresh token (el access token sigue en 15 min, tamaka.security.jwt)
 */
@ConfigurationProperties(prefix = "tamaka.cuentas")
public record CuentasProperties(Duration refreshTokenTtl) {

    public CuentasProperties {
        if (refreshTokenTtl == null) {
            refreshTokenTtl = Duration.ofDays(7);
        }
    }
}
