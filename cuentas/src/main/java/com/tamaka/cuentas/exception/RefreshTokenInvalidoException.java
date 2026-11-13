package com.tamaka.cuentas.exception;

import org.springframework.security.authentication.BadCredentialsException;

/**
 * Refresh token desconocido, expirado, revocado o de una cuenta suspendida. Es un fallo de autenticacion (401),
 * no una regla de negocio: el mensaje es generico para no revelar cual de los casos ocurrio.
 */
public class RefreshTokenInvalidoException extends BadCredentialsException {

    public RefreshTokenInvalidoException() {
        super("Refresh token invalido");
    }
}
