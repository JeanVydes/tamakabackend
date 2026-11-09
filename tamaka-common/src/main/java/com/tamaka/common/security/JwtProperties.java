package com.tamaka.common.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracion JWT (RS256). Las claves llegan por variables de entorno o AWS Secrets Manager;
 * nunca se versionan en el repositorio.
 *
 * @param issuer       emisor esperado en el claim iss (p. ej. "tamaka-cuentas")
 * @param publicKey    clave publica X.509 (SPKI) en Base64, usada por todos los servicios para validar
 * @param privateKey   clave privada PKCS#8 en Base64, usada solo por Cuentas para firmar
 * @param accessTokenTtl vida del access token (15 min segun la guia de seguridad)
 */
@ConfigurationProperties(prefix = "tamaka.security.jwt")
public record JwtProperties(
        String issuer,
        String publicKey,
        String privateKey,
        Duration accessTokenTtl
) {

    public JwtProperties {
        if (issuer == null || issuer.isBlank()) {
            issuer = "tamaka-cuentas";
        }
        if (accessTokenTtl == null) {
            accessTokenTtl = Duration.ofMinutes(15);
        }
    }
}
