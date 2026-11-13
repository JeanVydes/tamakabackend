package com.tamaka.cuentas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.tamaka.cuentas.domain.Cuenta;
import com.tamaka.cuentas.domain.RefreshToken;
import com.tamaka.cuentas.exception.RefreshTokenInvalidoException;
import com.tamaka.cuentas.repository.CuentaRepository;
import com.tamaka.cuentas.repository.RefreshTokenRepository;

@ExtendWith(MockitoExtension.class)
class SesionServiceImplTest {

    private static final Instant AHORA = Instant.parse("2026-11-09T10:00:00Z");

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private EmisorTokens emisorTokens;

    private SesionServiceImpl sesionService;

    @BeforeEach
    void crear() {
        sesionService = new SesionServiceImpl(refreshTokenRepository, cuentaRepository, emisorTokens,
                new CuentasProperties(Duration.ofDays(7)), Clock.fixed(AHORA, ZoneOffset.UTC));
    }

    @Test
    void abrirSesionGuardaSoloElHashYDevuelveElPar() {
        when(emisorTokens.emitirAccessToken(any(), anyCollection())).thenReturn("jwt");
        when(emisorTokens.accessTokenTtlSegundos()).thenReturn(900L);

        var par = sesionService.abrirSesion(1L, List.of("TURISTA"), false);

        ArgumentCaptor<RefreshToken> guardado = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(guardado.capture());
        assertThat(par.accessToken()).isEqualTo("jwt");
        assertThat(par.expiresInSeconds()).isEqualTo(900L);
        assertThat(par.refreshToken()).isNotBlank();
        assertThat(par.refreshExpiresInSeconds()).isEqualTo(Duration.ofDays(7).toSeconds());
        assertThat(guardado.getValue().getTokenHash())
                .isEqualTo(SesionServiceImpl.hash(par.refreshToken()))
                .isNotEqualTo(par.refreshToken());
        assertThat(guardado.getValue().getExpiraEn()).isEqualTo(AHORA.plus(Duration.ofDays(7)));
        verify(refreshTokenRepository, never()).revocarActivos(anyLong(), any());
    }

    @Test
    void cambioDeModoRevocaLasSesionesAnteriores() {
        when(emisorTokens.emitirAccessToken(any(), anyCollection())).thenReturn("jwt");

        sesionService.abrirSesion(1L, List.of("TURISTA", "OPERADOR"), true);

        verify(refreshTokenRepository).revocarActivos(1L, AHORA);
    }

    @Test
    void renovarRotaElTokenYUsaLosRolesVigentes() {
        RefreshToken actual = new RefreshToken(1L, SesionServiceImpl.hash("rt-1"), AHORA.minusSeconds(60),
                AHORA.plus(Duration.ofDays(1)));
        when(refreshTokenRepository.findByTokenHash(SesionServiceImpl.hash("rt-1"))).thenReturn(Optional.of(actual));
        when(cuentaRepository.findConRolesPorId(1L)).thenReturn(Optional.of(cuentaActiva()));
        when(emisorTokens.emitirAccessToken(any(), anyCollection())).thenReturn("jwt-nuevo");

        var par = sesionService.renovar("rt-1");

        assertThat(actual.isRevocado()).isTrue();
        assertThat(par.accessToken()).isEqualTo("jwt-nuevo");
        assertThat(par.refreshToken()).isNotEqualTo("rt-1");
        verify(emisorTokens).emitirAccessToken(1L, List.of("TURISTA"));
    }

    @Test
    void reutilizarUnTokenRevocadoCierraTodasLasSesiones() {
        RefreshToken rotado = new RefreshToken(1L, SesionServiceImpl.hash("rt-viejo"), AHORA.minusSeconds(600),
                AHORA.plus(Duration.ofDays(1)));
        rotado.revocar(AHORA.minusSeconds(300));
        when(refreshTokenRepository.findByTokenHash(SesionServiceImpl.hash("rt-viejo"))).thenReturn(Optional.of(rotado));

        assertThatThrownBy(() -> sesionService.renovar("rt-viejo")).isInstanceOf(RefreshTokenInvalidoException.class);

        verify(refreshTokenRepository).revocarActivos(1L, AHORA);
        verify(emisorTokens, never()).emitirAccessToken(any(), anyCollection());
    }

    @Test
    void tokenExpiradoODesconocidoEsInvalido() {
        RefreshToken expirado = new RefreshToken(1L, SesionServiceImpl.hash("rt-exp"), AHORA.minus(Duration.ofDays(8)),
                AHORA.minusSeconds(1));
        when(refreshTokenRepository.findByTokenHash(SesionServiceImpl.hash("rt-exp"))).thenReturn(Optional.of(expirado));
        when(refreshTokenRepository.findByTokenHash(SesionServiceImpl.hash("otro"))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sesionService.renovar("rt-exp")).isInstanceOf(RefreshTokenInvalidoException.class);
        assertThatThrownBy(() -> sesionService.renovar("otro")).isInstanceOf(RefreshTokenInvalidoException.class);
    }

    @Test
    void cuentaSuspendidaNoRenueva() {
        RefreshToken actual = new RefreshToken(1L, SesionServiceImpl.hash("rt-1"), AHORA.minusSeconds(60),
                AHORA.plus(Duration.ofDays(1)));
        Cuenta suspendida = cuentaActiva();
        suspendida.suspender();
        when(refreshTokenRepository.findByTokenHash(SesionServiceImpl.hash("rt-1"))).thenReturn(Optional.of(actual));
        when(cuentaRepository.findConRolesPorId(1L)).thenReturn(Optional.of(suspendida));

        assertThatThrownBy(() -> sesionService.renovar("rt-1")).isInstanceOf(RefreshTokenInvalidoException.class);
        verify(refreshTokenRepository).revocarActivos(1L, AHORA);
    }

    @Test
    void logoutRevocaElTokenYEsIdempotente() {
        RefreshToken actual = new RefreshToken(1L, SesionServiceImpl.hash("rt-1"), AHORA.minusSeconds(60),
                AHORA.plus(Duration.ofDays(1)));
        when(refreshTokenRepository.findByTokenHash(SesionServiceImpl.hash("rt-1"))).thenReturn(Optional.of(actual));
        when(refreshTokenRepository.findByTokenHash(SesionServiceImpl.hash("desconocido"))).thenReturn(Optional.empty());

        sesionService.cerrarSesion("rt-1");
        sesionService.cerrarSesion("desconocido");

        assertThat(actual.getRevocadoEn()).isEqualTo(AHORA);
    }

    private static Cuenta cuentaActiva() {
        Cuenta cuenta = Cuenta.registrar("ana@tamaka.co", "hash", "Ana");
        ReflectionTestUtils.setField(cuenta, "id", 1L);
        return cuenta;
    }
}
