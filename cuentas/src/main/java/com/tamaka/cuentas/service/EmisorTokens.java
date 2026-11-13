package com.tamaka.cuentas.service;

import java.util.Collection;

/**
 * Puerto hacia la capa de seguridad para firmar access tokens (JWT RS256). Lo implementa
 * {@code security.EmisorAccessTokenJwt}; la gestion de sesiones (refresh tokens) vive en {@link SesionService}.
 */
public interface EmisorTokens {

    String emitirAccessToken(Long cuentaId, Collection<String> roles);

    long accessTokenTtlSegundos();
}
