package com.tamaka.cuentas.dto.response;

/**
 * Par de tokens: access token JWT (15 min) y refresh token opaco (rotado en cada uso).
 */
public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        String refreshToken,
        long refreshExpiresInSeconds
) {
}
