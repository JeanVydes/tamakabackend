package com.tamaka.cuentas.service;

import java.util.Locale;
import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tamaka.common.error.ResourceNotFoundException;
import com.tamaka.common.event.DomainEvent;
import com.tamaka.common.event.EventPublisher;
import com.tamaka.cuentas.domain.Cuenta;
import com.tamaka.cuentas.domain.Rol;
import com.tamaka.cuentas.dto.request.RegistrarCuentaRequest;
import com.tamaka.cuentas.dto.request.SuspensionRequest;
import com.tamaka.cuentas.dto.response.CuentaResponse;
import com.tamaka.cuentas.exception.EmailYaRegistradoException;
import com.tamaka.cuentas.mapper.CuentaMapper;
import com.tamaka.cuentas.repository.CuentaRepository;

@Service
@Transactional(readOnly = true)
public class CuentaServiceImpl implements CuentaService {

    private static final String AGREGADO = "Cuenta";

    private final CuentaRepository cuentaRepository;
    private final CuentaMapper cuentaMapper;
    private final PasswordEncoder passwordEncoder;
    private final EventPublisher eventPublisher;

    public CuentaServiceImpl(CuentaRepository cuentaRepository,
                             CuentaMapper cuentaMapper,
                             PasswordEncoder passwordEncoder,
                             EventPublisher eventPublisher) {
        this.cuentaRepository = cuentaRepository;
        this.cuentaMapper = cuentaMapper;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public CuentaResponse registrar(RegistrarCuentaRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (cuentaRepository.existsByEmail(email)) {
            throw new EmailYaRegistradoException(email);
        }
        Cuenta cuenta = Cuenta.registrar(email, passwordEncoder.encode(request.password()), request.nombre().trim());
        return cuentaMapper.toResponse(cuentaRepository.save(cuenta));
    }

    @Override
    public CuentaResponse buscarPorId(Long cuentaId) {
        return cuentaMapper.toResponse(obtener(cuentaId));
    }

    @Override
    @Transactional
    public CuentaResponse activarModoOperador(Long cuentaId) {
        Cuenta cuenta = obtener(cuentaId);
        cuenta.agregarRol(Rol.OPERADOR);
        return cuentaMapper.toResponse(cuenta);
    }

    @Override
    @Transactional
    public CuentaResponse cambiarSuspension(Long cuentaId, SuspensionRequest request) {
        Cuenta cuenta = obtener(cuentaId);
        if (Boolean.TRUE.equals(request.suspendida())) {
            cuenta.suspender();
            eventPublisher.publicar(DomainEvent.de("CuentaSuspendida", cuentaId,
                    Map.of("motivo", request.motivo() == null ? "" : request.motivo())));
        } else {
            cuenta.reactivar();
        }
        return cuentaMapper.toResponse(cuenta);
    }

    @Override
    @Transactional
    public CuentaResponse asignarRolGestor(Long cuentaId) {
        Cuenta cuenta = obtener(cuentaId);
        cuenta.agregarRol(Rol.GESTOR);
        eventPublisher.publicar(DomainEvent.de("RolCambiado", cuentaId, Map.of("rol", Rol.GESTOR.name())));
        return cuentaMapper.toResponse(cuenta);
    }

    private Cuenta obtener(Long cuentaId) {
        return cuentaRepository.findConRolesPorId(cuentaId)
                .orElseThrow(() -> new ResourceNotFoundException(AGREGADO, cuentaId));
    }
}
