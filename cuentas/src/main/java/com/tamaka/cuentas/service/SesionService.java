package com.tamaka.cuentas.service;

import java.util.Collection;

import com.tamaka.cuentas.dto.response.TokenResponse;

/**
 * Sesiones: par access token (15 min) + refresh token opaco persistido (hash, expiracion, rotacion, revocacion).
 */
public interface SesionService {

    /**
     * Emite un par nuevo para la cuenta con los roles dados. Con {@code revocarAnteriores} invalida antes todos sus
     * refresh tokens (p. ej., al cambiar de modo: los roles viejos no deben poder renovarse).
     */
    TokenResponse abrirSesion(Long cuentaId, Collection<String> roles, boolean revocarAnteriores);

    /**
     * Rotacion: valida el refresh token, lo revoca y emite un par nuevo con los roles vigentes de la cuenta.
     * Reutilizar un token ya revocado revoca todas las sesiones de la cuenta (posible robo).
     */
    TokenResponse renovar(String refreshToken);

    /** Logout: revoca el refresh token. Idempotente; un token desconocido no revela nada. */
    void cerrarSesion(String refreshToken);

    /** Revoca todas las sesiones de la cuenta (suspension). */
    void revocarTodas(Long cuentaId);
}
