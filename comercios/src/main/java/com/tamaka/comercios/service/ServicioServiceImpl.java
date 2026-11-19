package com.tamaka.comercios.service;

import java.util.Map;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tamaka.common.error.ResourceNotFoundException;
import com.tamaka.common.event.DomainEvent;
import com.tamaka.common.event.EventPublisher;
import com.tamaka.common.storage.ImageStorage;
import com.tamaka.comercios.domain.Servicio;
import com.tamaka.comercios.dto.request.ActualizarServicioRequest;
import com.tamaka.comercios.dto.request.CambiarActivoRequest;
import com.tamaka.comercios.dto.response.ServicioResponse;
import com.tamaka.comercios.exception.RangoPersonasInvalidoException;
import com.tamaka.comercios.mapper.CatalogoMapper;
import com.tamaka.comercios.repository.ServicioRepository;

@Service
@Transactional(readOnly = true)
public class ServicioServiceImpl implements ServicioService {

    private final ServicioRepository servicioRepository;
    private final CatalogoMapper catalogoMapper;
    private final EventPublisher eventPublisher;
    private final ImageStorage imageStorage;

    public ServicioServiceImpl(ServicioRepository servicioRepository,
                               CatalogoMapper catalogoMapper,
                               EventPublisher eventPublisher,
                               ImageStorage imageStorage) {
        this.servicioRepository = servicioRepository;
        this.catalogoMapper = catalogoMapper;
        this.eventPublisher = eventPublisher;
        this.imageStorage = imageStorage;
    }

    @Override
    public ServicioResponse obtener(Long servicioId) {
        return catalogoMapper.toResponse(cargar(servicioId));
    }

    @Override
    @Transactional
    public ServicioResponse cambiarActivo(Long operadorId, Long servicioId, CambiarActivoRequest request) {
        Servicio servicio = cargarPropio(operadorId, servicioId);
        boolean nuevo = request.activo();
        boolean desactivando = !nuevo && servicio.isActivo();
        boolean reactivando = nuevo && !servicio.isActivo();
        servicio.cambiarActivo(nuevo);
        if (desactivando) {
            eventPublisher.publicar(DomainEvent.de("ServicioDesactivado", servicioId, Map.of(
                    "comercioId", servicio.getComercio().getId(),
                    "operadorId", operadorId,
                    "motivo", "DESACTIVADO_POR_OPERADOR")));
        } else if (reactivando) {
            eventPublisher.publicar(EventosCatalogo.servicio(EventosCatalogo.SERVICIO_ACTUALIZADO, servicio));
        }
        return catalogoMapper.toResponse(servicio);
    }

    @Override
    @Transactional
    public ServicioResponse actualizar(Long operadorId, Long servicioId, ActualizarServicioRequest request) {
        Servicio servicio = cargarPropio(operadorId, servicioId);
        int minimo = request.minPersonas() != null ? request.minPersonas() : servicio.getMinPersonas();
        int maximo = request.maxPersonas() != null ? request.maxPersonas() : servicio.getMaxPersonas();
        if (maximo < minimo) {
            throw new RangoPersonasInvalidoException(minimo, maximo);
        }
        String nombre = request.nombre() == null ? null : request.nombre().trim();
        servicio.actualizar(nombre, request.precioBase(), request.minPersonas(), request.maxPersonas(),
                request.atributos());
        if (servicio.isActivo()) {
            eventPublisher.publicar(EventosCatalogo.servicio(EventosCatalogo.SERVICIO_ACTUALIZADO, servicio));
        }
        return catalogoMapper.toResponse(servicio);
    }

    @Override
    @Transactional
    public ServicioResponse subirImagen(Long operadorId, Long servicioId, MultipartFile archivo) {
        Servicio servicio = cargarPropio(operadorId, servicioId);
        // subir valida tipo/firma/5MB y devuelve la clave del objeto ya guardado en R2
        String claveNueva = imageStorage.subir("servicios", String.valueOf(servicio.getId()), archivo);
        String vieja = servicio.getImagenKey();
        servicio.cambiarImagen(claveNueva);
        servicioRepository.save(servicio);
        imageStorage.borrar(vieja);
        return catalogoMapper.toResponse(servicio);
    }

    @Override
    @Transactional
    public ServicioResponse eliminarImagen(Long operadorId, Long servicioId) {
        Servicio servicio = cargarPropio(operadorId, servicioId);
        String vieja = servicio.getImagenKey();
        servicio.cambiarImagen(null);
        servicioRepository.save(servicio);
        imageStorage.borrar(vieja);
        return catalogoMapper.toResponse(servicio);
    }

    /** Regla de pertenencia: solo el operador dueno del comercio modifica sus servicios. */
    private Servicio cargarPropio(Long operadorId, Long servicioId) {
        Servicio servicio = cargar(servicioId);
        if (!servicio.getComercio().esDe(operadorId)) {
            throw new AccessDeniedException("El servicio no pertenece al operador autenticado");
        }
        return servicio;
    }

    private Servicio cargar(Long servicioId) {
        return servicioRepository.findConComercioPorId(servicioId)
                .orElseThrow(() -> new ResourceNotFoundException("Servicio", servicioId));
    }
}
