package com.tamaka.cuentas.security;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.tamaka.common.security.JwtService;
import com.tamaka.cuentas.dto.request.LoginRequest;
import com.tamaka.cuentas.dto.response.TokenResponse;

/**
 * Login: verifica credenciales con el AuthenticationManager y emite el access token RS256.
 * Solo Cuentas emite tokens.
 */
@Service
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthenticationService(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public TokenResponse login(LoginRequest request) {
        Authentication autenticacion = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(request.email().trim(), request.password()));
        CuentaPrincipal principal = (CuentaPrincipal) autenticacion.getPrincipal();
        String token = jwtService.issue(String.valueOf(principal.getId()), principal.getRolesNombres());
        return new TokenResponse(token, "Bearer", jwtService.accessTokenTtlSeconds());
    }
}
