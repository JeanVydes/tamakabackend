package com.tamaka.cuentas.dto.response;

/**
 * Cuenta actualizada mas un access token nuevo con los roles del modo activo.
 * El cliente debe reemplazar su token: el anterior conserva los roles viejos hasta expirar.
 */
public record SesionResponse(
        CuentaResponse cuenta,
        TokenResponse token
) {
}
