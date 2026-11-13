package com.tamaka.cuentas.security;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Component;

import com.tamaka.common.security.JwtService;
import com.tamaka.cuentas.service.EmisorTokens;

/**
 * Firma access tokens RS256 con la clave privada (solo Cuentas emite tokens).
 */
@Component
public class EmisorAccessTokenJwt implements EmisorTokens {

    private final JwtService jwtService;

    public EmisorAccessTokenJwt(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public String emitirAccessToken(Long cuentaId, Collection<String> roles) {
        return jwtService.issue(String.valueOf(cuentaId), List.copyOf(roles));
    }

    @Override
    public long accessTokenTtlSegundos() {
        return jwtService.accessTokenTtlSeconds();
    }
}
