package com.tamaka.cuentas.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tamaka.cuentas.dto.request.LoginRequest;
import com.tamaka.cuentas.dto.request.RefreshTokenRequest;
import com.tamaka.cuentas.dto.response.TokenResponse;
import com.tamaka.cuentas.security.AuthenticationService;
import com.tamaka.cuentas.service.SesionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final SesionService sesionService;

    public AuthenticationController(AuthenticationService authenticationService, SesionService sesionService) {
        this.authenticationService = authenticationService;
        this.sesionService = sesionService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authenticationService.login(request));
    }

    /** Rotacion: el refresh token usado queda revocado y se devuelve un par nuevo. 401 si no es valido. */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(sesionService.renovar(request.refreshToken()));
    }

    /** Revoca el refresh token. Idempotente: siempre 204. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        sesionService.cerrarSesion(request.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
