package com.tamaka.cuentas.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tamaka.common.error.ResourceNotFoundException;
import com.tamaka.common.event.DomainEvent;
import com.tamaka.common.event.EventPublisher;
import com.tamaka.cuentas.domain.Cuenta;
import com.tamaka.cuentas.domain.Rol;
import com.tamaka.cuentas.dto.request.CambiarModoRequest;
import com.tamaka.cuentas.dto.request.RegistrarCuentaRequest;
import com.tamaka.cuentas.dto.request.SuspensionRequest;
import com.tamaka.cuentas.dto.response.CuentaResponse;
import com.tamaka.cuentas.dto.response.SesionResponse;
import com.tamaka.cuentas.exception.CuentaSuspendidaException;
import com.tamaka.cuentas.exception.EmailYaRegistradoException;
import com.tamaka.cuentas.mapper.CuentaMapper;
import com.tamaka.cuentas.repository.CuentaRepository;
import com.tamaka.common.storage.ImageStorage;

@Service
@Transactional(readOnly = true)
public class CuentaServiceImpl implements CuentaService {

    private static final String AGREGADO = "Cuenta";

    private final CuentaRepository cuentaRepository;
    private final CuentaMapper cuentaMapper;
    private final PasswordEncoder passwordEncoder;
    private final EventPublisher eventPublisher;
    private final SesionService sesionService;
    private final ImageStorage imageStorage;

    public CuentaServiceImpl(CuentaRepository cuentaRepository,
                             CuentaMapper cuentaMapper,
                             PasswordEncoder passwordEncoder,
                             EventPublisher eventPublisher,
                             SesionService sesionService,
                             ImageStorage imageStorage) {
        this.cuentaRepository = cuentaRepository;
        this.cuentaMapper = cuentaMapper;
        this.passwordEncoder = passwordEncoder;
        this.eventPublisher = eventPublisher;
        this.sesionService = sesionService;
        this.imageStorage = imageStorage;
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
    public CuentaResponse subirFoto(Long cuentaId, MultipartFile archivo) {
        Cuenta cuenta = obtener(cuentaId);
        // subir valida tipo/firma/5MB y devuelve la clave del objeto ya guardado en R2
        String claveNueva = imageStorage.subir("usuarios", String.valueOf(cuenta.getId()), archivo);
        String vieja = cuenta.getImagenKey();
        cuenta.cambiarImagen(claveNueva);
        cuentaRepository.save(cuenta);
        imageStorage.borrar(vieja);
        return cuentaMapper.toResponse(cuenta);
    }

    @Override
    @Transactional
    public CuentaResponse eliminarFoto(Long cuentaId) {
        Cuenta cuenta = obtener(cuentaId);
        String vieja = cuenta.getImagenKey();
        cuenta.cambiarImagen(null);
        cuentaRepository.save(cuenta);
        imageStorage.borrar(vieja);
        return cuentaMapper.toResponse(cuenta);
    }

    @Override
    public List<CuentaResponse> listar() {
        return cuentaRepository.findAllByOrderByCreadaEnDesc().stream()
                .map(cuentaMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public SesionResponse activarModoOperador(Long cuentaId) {
        Cuenta cuenta = obtenerActiva(cuentaId);
        if (cuenta.activarModoOperador()) {
            eventPublisher.publicar(DomainEvent.de("RolCambiado", cuentaId, Map.of("rol", Rol.OPERADOR.name())));
        }
        return sesion(cuenta);
    }

    @Override
    @Transactional
    public SesionResponse cambiarModo(Long cuentaId, CambiarModoRequest request) {
        Cuenta cuenta = obtenerActiva(cuentaId);
        cuenta.cambiarModo(Rol.OPERADOR.name().equals(request.modo()));
        return sesion(cuenta);
    }

    @Override
    @Transactional
    public CuentaResponse cambiarSuspension(Long cuentaId, SuspensionRequest request) {
        Cuenta cuenta = obtener(cuentaId);
        if (Boolean.TRUE.equals(request.suspendida())) {
            cuenta.suspender();
            sesionService.revocarTodas(cuentaId);
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

    /**
     * Respuesta con la cuenta y un par de tokens nuevo con los roles del modo activo. Los refresh tokens anteriores
     * se revocan: no deben poder renovar un access token con los roles del modo previo.
     */
    private SesionResponse sesion(Cuenta cuenta) {
        List<String> roles = cuenta.rolesActivos().stream().map(Enum::name).toList();
        return new SesionResponse(cuentaMapper.toResponse(cuenta), sesionService.abrirSesion(cuenta.getId(), roles, true));
    }

    /** Una cuenta suspendida no cambia de modo ni recibe tokens nuevos. */
    private Cuenta obtenerActiva(Long cuentaId) {
        Cuenta cuenta = obtener(cuentaId);
        if (!cuenta.isActiva()) {
            throw new CuentaSuspendidaException(cuentaId);
        }
        return cuenta;
    }

    private Cuenta obtener(Long cuentaId) {
        return cuentaRepository.findConRolesPorId(cuentaId)
                .orElseThrow(() -> new ResourceNotFoundException(AGREGADO, cuentaId));
    }
}
