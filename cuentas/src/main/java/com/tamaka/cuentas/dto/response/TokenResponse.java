package com.tamaka.cuentas.dto.response;

public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds
) {
}
