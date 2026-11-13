package com.tamaka.cuentas.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.HexFormat;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tamaka.cuentas.domain.Cuenta;
import com.tamaka.cuentas.domain.RefreshToken;
import com.tamaka.cuentas.dto.response.TokenResponse;
import com.tamaka.cuentas.exception.RefreshTokenInvalidoException;
import com.tamaka.cuentas.repository.CuentaRepository;
import com.tamaka.cuentas.repository.RefreshTokenRepository;

@Service
@Transactional
public class SesionServiceImpl implements SesionService {

    private static final Logger log = LoggerFactory.getLogger(SesionServiceImpl.class);
    private static final int BYTES_TOKEN = 32;
    private static final String TIPO_TOKEN = "Bearer";

    private final RefreshTokenRepository refreshTokenRepository;
    private final CuentaRepository cuentaRepository;
    private final EmisorTokens emisorTokens;
    private final CuentasProperties properties;
    private final Clock reloj;
    private final SecureRandom aleatorio = new SecureRandom();

    @Autowired
    public SesionServiceImpl(RefreshTokenRepository refreshTokenRepository,
                             CuentaRepository cuentaRepository,
                             EmisorTokens emisorTokens,
                             CuentasProperties properties) {
        this(refreshTokenRepository, cuentaRepository, emisorTokens, properties, Clock.systemUTC());
    }

    SesionServiceImpl(RefreshTokenRepository refreshTokenRepository,
                      CuentaRepository cuentaRepository,
                      EmisorTokens emisorTokens,
                      CuentasProperties properties,
                      Clock reloj) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.cuentaRepository = cuentaRepository;
        this.emisorTokens = emisorTokens;
        this.properties = properties;
        this.reloj = reloj;
    }

    @Override
    public TokenResponse abrirSesion(Long cuentaId, Collection<String> roles, boolean revocarAnteriores) {
        Instant ahora = reloj.instant();
        if (revocarAnteriores) {
            refreshTokenRepository.revocarActivos(cuentaId, ahora);
        }
        return emitirPar(cuentaId, roles, ahora);
    }

    @Override
    public TokenResponse renovar(String refreshToken) {
        Instant ahora = reloj.instant();
        RefreshToken guardado = refreshTokenRepository.findByTokenHash(hash(refreshToken))
                .orElseThrow(RefreshTokenInvalidoException::new);
        if (guardado.isRevocado()) {
            // Reutilizacion de un token ya rotado: alguien mas lo tiene. Se cierran todas las sesiones.
            log.warn("reutilizacion de refresh token revocado cuenta={}", guardado.getCuentaId());
            refreshTokenRepository.revocarActivos(guardado.getCuentaId(), ahora);
            throw new RefreshTokenInvalidoException();
        }
        if (!guardado.vigenteEn(ahora)) {
            throw new RefreshTokenInvalidoException();
        }
        Cuenta cuenta = cuentaRepository.findConRolesPorId(guardado.getCuentaId())
                .orElseThrow(RefreshTokenInvalidoException::new);
        if (!cuenta.isActiva()) {
            refreshTokenRepository.revocarActivos(cuenta.getId(), ahora);
            throw new RefreshTokenInvalidoException();
        }
        guardado.revocar(ahora);
        List<String> roles = cuenta.rolesActivos().stream().map(Enum::name).toList();
        return emitirPar(cuenta.getId(), roles, ahora);
    }

    @Override
    public void cerrarSesion(String refreshToken) {
        refreshTokenRepository.findByTokenHash(hash(refreshToken))
                .ifPresent(token -> token.revocar(reloj.instant()));
    }

    @Override
    public void revocarTodas(Long cuentaId) {
        refreshTokenRepository.revocarActivos(cuentaId, reloj.instant());
    }

    private TokenResponse emitirPar(Long cuentaId, Collection<String> roles, Instant ahora) {
        String refresh = nuevoTokenOpaco();
        Instant expira = ahora.plus(properties.refreshTokenTtl());
        refreshTokenRepository.save(new RefreshToken(cuentaId, hash(refresh), ahora, expira));
        String access = emisorTokens.emitirAccessToken(cuentaId, roles.stream().sorted().toList());
        return new TokenResponse(access, TIPO_TOKEN, emisorTokens.accessTokenTtlSegundos(),
                refresh, properties.refreshTokenTtl().toSeconds());
    }

    private String nuevoTokenOpaco() {
        byte[] bytes = new byte[BYTES_TOKEN];
        aleatorio.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 en hexadecimal: es lo unico que se guarda del refresh token. */
    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }
}
