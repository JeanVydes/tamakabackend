package com.tamaka.cuentas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.tamaka.common.event.EventPublisher;
import com.tamaka.cuentas.domain.Cuenta;
import com.tamaka.cuentas.dto.request.CambiarModoRequest;
import com.tamaka.cuentas.dto.request.RegistrarCuentaRequest;
import com.tamaka.cuentas.dto.request.SuspensionRequest;
import com.tamaka.cuentas.dto.response.TokenResponse;
import com.tamaka.cuentas.exception.CuentaSuspendidaException;
import com.tamaka.cuentas.exception.CuentaYaSuspendidaException;
import com.tamaka.cuentas.exception.EmailYaRegistradoException;
import com.tamaka.cuentas.exception.ModoOperadorNoHabilitadoException;
import com.tamaka.cuentas.mapper.CuentaMapper;
import com.tamaka.cuentas.repository.CuentaRepository;

@ExtendWith(MockitoExtension.class)
class CuentaServiceImplTest {

    @Mock
    private CuentaRepository cuentaRepository;

    @Spy
    private CuentaMapper cuentaMapper = new CuentaMapper();

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private SesionService sesionService;

    @InjectMocks
    private CuentaServiceImpl cuentaService;

    @Test
    void registraCuentaConEmailNormalizadoYRolTurista() {
        when(cuentaRepository.existsByEmail("ana@tamaka.co")).thenReturn(false);
        when(passwordEncoder.encode("clave-segura")).thenReturn("hash");
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(inv -> inv.getArgument(0));

        var resultado = cuentaService.registrar(
                new RegistrarCuentaRequest(" Ana@TAMAKA.co ", "clave-segura", " Ana "));

        assertThat(resultado.email()).isEqualTo("ana@tamaka.co");
        assertThat(resultado.nombre()).isEqualTo("Ana");
        assertThat(resultado.roles()).containsExactly("TURISTA");
        verify(cuentaRepository).save(any(Cuenta.class));
    }

    @Test
    void rechazaEmailDuplicadoSinGuardar() {
        when(cuentaRepository.existsByEmail("ana@tamaka.co")).thenReturn(true);

        assertThatThrownBy(() -> cuentaService.registrar(
                new RegistrarCuentaRequest("ana@tamaka.co", "clave-segura", "Ana")))
                .isInstanceOf(EmailYaRegistradoException.class);

        verify(cuentaRepository, never()).save(any());
    }

    @Test
    void suspendeCuentaYPublicaEvento() {
        Cuenta cuenta = Cuenta.registrar("ana@tamaka.co", "hash", "Ana");
        when(cuentaRepository.findConRolesPorId(1L)).thenReturn(Optional.of(cuenta));

        var resultado = cuentaService.cambiarSuspension(1L, new SuspensionRequest(true, "fraude"));

        assertThat(resultado.activa()).isFalse();
        verify(eventPublisher).publicar(any());
    }

    @Test
    void noSuspendeDosVeces() {
        Cuenta cuenta = Cuenta.registrar("ana@tamaka.co", "hash", "Ana");
        cuenta.suspender();
        when(cuentaRepository.findConRolesPorId(1L)).thenReturn(Optional.of(cuenta));

        assertThatThrownBy(() -> cuentaService.cambiarSuspension(1L, new SuspensionRequest(true, null)))
                .isInstanceOf(CuentaYaSuspendidaException.class);
    }

    @Test
    void activarModoOperadorEmiteTokenConRolOperadorYPublicaRolCambiado() {
        Cuenta cuenta = Cuenta.registrar("ana@tamaka.co", "hash", "Ana");
        when(cuentaRepository.findConRolesPorId(1L)).thenReturn(Optional.of(cuenta));
        when(sesionService.abrirSesion(any(), any(), eq(true))).thenReturn(new TokenResponse("tkn", "Bearer", 900, "rt", 604800));

        var sesion = cuentaService.activarModoOperador(1L);

        assertThat(sesion.token().accessToken()).isEqualTo("tkn");
        assertThat(sesion.cuenta().modoOperador()).isTrue();
        verify(sesionService).abrirSesion(any(), argThat(roles -> roles.containsAll(List.of("TURISTA", "OPERADOR"))), eq(true));
        verify(eventPublisher).publicar(any());
    }

    @Test
    void volverAModoTuristaQuitaOperadorDelToken() {
        Cuenta cuenta = Cuenta.registrar("ana@tamaka.co", "hash", "Ana");
        cuenta.activarModoOperador();
        when(cuentaRepository.findConRolesPorId(1L)).thenReturn(Optional.of(cuenta));
        when(sesionService.abrirSesion(any(), any(), eq(true))).thenReturn(new TokenResponse("tkn", "Bearer", 900, "rt", 604800));

        var sesion = cuentaService.cambiarModo(1L, new CambiarModoRequest("TURISTA"));

        assertThat(sesion.cuenta().modoOperador()).isFalse();
        assertThat(sesion.cuenta().roles()).contains("OPERADOR");
        verify(sesionService).abrirSesion(any(), argThat(roles -> !roles.contains("OPERADOR") && roles.contains("TURISTA")), eq(true));
        verify(eventPublisher, never()).publicar(any());
    }

    @Test
    void noPasaAModoOperadorSinElRolHabilitado() {
        Cuenta cuenta = Cuenta.registrar("ana@tamaka.co", "hash", "Ana");
        when(cuentaRepository.findConRolesPorId(1L)).thenReturn(Optional.of(cuenta));

        assertThatThrownBy(() -> cuentaService.cambiarModo(1L, new CambiarModoRequest("OPERADOR")))
                .isInstanceOf(ModoOperadorNoHabilitadoException.class);
        verify(sesionService, never()).abrirSesion(any(), any(), anyBoolean());
    }

    @Test
    void cuentaSuspendidaNoRecibeTokenNuevo() {
        Cuenta cuenta = Cuenta.registrar("ana@tamaka.co", "hash", "Ana");
        cuenta.suspender();
        when(cuentaRepository.findConRolesPorId(1L)).thenReturn(Optional.of(cuenta));

        assertThatThrownBy(() -> cuentaService.activarModoOperador(1L))
                .isInstanceOf(CuentaSuspendidaException.class);
        verify(sesionService, never()).abrirSesion(any(), any(), anyBoolean());
    }
}
