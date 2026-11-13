package com.tamaka.cuentas.security;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.tamaka.cuentas.dto.request.LoginRequest;
import com.tamaka.cuentas.dto.response.TokenResponse;
import com.tamaka.cuentas.service.SesionService;

/**
 * Login: verifica credenciales con el AuthenticationManager y abre una sesion (access token RS256 + refresh token).
 */
@Service
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final SesionService sesionService;

    public AuthenticationService(AuthenticationManager authenticationManager, SesionService sesionService) {
        this.authenticationManager = authenticationManager;
        this.sesionService = sesionService;
    }

    public TokenResponse login(LoginRequest request) {
        Authentication autenticacion = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.email().trim(), request.password()));
        CuentaPrincipal principal = (CuentaPrincipal) autenticacion.getPrincipal();
        return sesionService.abrirSesion(principal.getId(), principal.getRolesNombres(), false);
    }
}
