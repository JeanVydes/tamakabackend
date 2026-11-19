package com.tamaka.comercios.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tamaka.common.error.ResourceNotFoundException;
import com.tamaka.common.event.DomainEvent;
import com.tamaka.common.event.EventPublisher;
import com.tamaka.common.storage.ImageStorage;
import com.tamaka.comercios.domain.Comercio;
import com.tamaka.comercios.domain.Servicio;
import com.tamaka.comercios.dto.request.ActualizarComercioRequest;
import com.tamaka.comercios.dto.request.CrearComercioRequest;
import com.tamaka.comercios.dto.request.CrearServicioRequest;
import com.tamaka.comercios.dto.response.ComercioDetalleResponse;
import com.tamaka.comercios.dto.response.ComercioResponse;
import com.tamaka.comercios.dto.response.ServicioResponse;
import com.tamaka.comercios.exception.RangoPersonasInvalidoException;
import com.tamaka.comercios.exception.RntDuplicadoException;
import com.tamaka.comercios.mapper.CatalogoMapper;
import com.tamaka.comercios.repository.ComercioRepository;
import com.tamaka.comercios.repository.ServicioRepository;

@Service
@Transactional(readOnly = true)
public class ComercioServiceImpl implements ComercioService {

    private final ComercioRepository comercioRepository;
    private final ServicioRepository servicioRepository;
    private final CatalogoMapper catalogoMapper;
    private final EventPublisher eventPublisher;
    private final ImageStorage imageStorage;

    public ComercioServiceImpl(ComercioRepository comercioRepository,
                               ServicioRepository servicioRepository,
                               CatalogoMapper catalogoMapper,
                               EventPublisher eventPublisher,
                               ImageStorage imageStorage) {
        this.comercioRepository = comercioRepository;
        this.servicioRepository = servicioRepository;
        this.catalogoMapper = catalogoMapper;
        this.eventPublisher = eventPublisher;
        this.imageStorage = imageStorage;
    }

    @Override
    @Transactional
    public ComercioResponse crear(Long operadorId, CrearComercioRequest request) {
        String rnt = request.rnt().trim().toUpperCase(Locale.ROOT);
        if (comercioRepository.existsByRnt(rnt)) {
            throw new RntDuplicadoException();
        }
        Comercio comercio = Comercio.crear(operadorId, request.nombre().trim(), rnt,
                request.categoria().trim(), request.descripcion());
        return catalogoMapper.toResponse(comercioRepository.save(comercio));
    }

    @Override
    @Transactional
    public ComercioResponse actualizar(Long operadorId, Long comercioId, ActualizarComercioRequest request) {
        Comercio comercio = obtenerPropio(operadorId, comercioId);
        comercio.actualizar(request.nombre().trim(), request.categoria().trim(), request.descripcion());
        eventPublisher.publicar(DomainEvent.de("ComercioActualizado", comercioId,
                Map.of("nombre", comercio.getNombre())));
        return catalogoMapper.toResponse(comercio);
    }

    @Override
    @Transactional
    public ServicioResponse agregarServicio(Long operadorId, Long comercioId, CrearServicioRequest request) {
        Comercio comercio = obtenerPropio(operadorId, comercioId);
        int minimo = request.minPersonas();
        int maximo = request.maxPersonas();
        if (maximo < minimo) {
            throw new RangoPersonasInvalidoException(minimo, maximo);
        }
        Servicio servicio = Servicio.crear(comercio, request.nombre().trim(), request.precioBase(),
                minimo, maximo, request.atributos());
        Servicio guardado = servicioRepository.save(servicio);
        eventPublisher.publicar(EventosCatalogo.servicio(EventosCatalogo.SERVICIO_CREADO, guardado));
        return catalogoMapper.toResponse(guardado);
    }

    @Override
    public List<ComercioResponse> buscar(String categoria, String texto) {
        String cat = categoria == null ? "" : categoria.trim();
        String txt = texto == null ? "" : texto.trim();
        return comercioRepository.buscarActivos(cat, txt).stream()
                .map(catalogoMapper::toResponse)
                .toList();
    }

    @Override
    public ComercioDetalleResponse obtener(Long comercioId) {
        Comercio comercio = comercioRepository.findById(comercioId)
                .filter(Comercio::isActivo)
                .orElseThrow(() -> new ResourceNotFoundException("Comercio", comercioId));
        return catalogoMapper.toDetalle(comercio, servicioRepository.findActivosDeComercio(comercioId));
    }

    @Override
    @Transactional
    public int desactivarPorCuentaSuspendida(Long operadorId) {
        comercioRepository.findByOperadorId(operadorId).forEach(Comercio::desactivar);
        List<Servicio> activos = servicioRepository.findActivosDeOperador(operadorId);
        for (Servicio servicio : activos) {
            servicio.cambiarActivo(false);
            eventPublisher.publicar(DomainEvent.de("ServicioDesactivado", servicio.getId(), Map.of(
                    "comercioId", servicio.getComercio().getId(),
                    "operadorId", operadorId,
                    "motivo", "CUENTA_SUSPENDIDA")));
        }
        return activos.size();
    }

    @Override
    @Transactional
    public ComercioResponse subirImagen(Long operadorId, Long comercioId, MultipartFile archivo) {
        Comercio comercio = obtenerPropio(operadorId, comercioId);
        // subir valida tipo/firma/5MB y devuelve la clave del objeto ya guardado en R2
        String claveNueva = imageStorage.subir("comercios", String.valueOf(comercio.getId()), archivo);
        String vieja = comercio.getImagenKey();
        comercio.cambiarImagen(claveNueva);
        comercioRepository.save(comercio);
        imageStorage.borrar(vieja);
        return catalogoMapper.toResponse(comercio);
    }

    @Override
    @Transactional
    public ComercioResponse eliminarImagen(Long operadorId, Long comercioId) {
        Comercio comercio = obtenerPropio(operadorId, comercioId);
        String vieja = comercio.getImagenKey();
        comercio.cambiarImagen(null);
        comercioRepository.save(comercio);
        imageStorage.borrar(vieja);
        return catalogoMapper.toResponse(comercio);
    }

    /**
     * Regla de pertenencia: solo el operador dueno del comercio puede modificarlo.
     */
    private Comercio obtenerPropio(Long operadorId, Long comercioId) {
        Comercio comercio = comercioRepository.findById(comercioId)
                .orElseThrow(() -> new ResourceNotFoundException("Comercio", comercioId));
        if (!comercio.esDe(operadorId)) {
            throw new AccessDeniedException("El comercio no pertenece al operador autenticado");
        }
        return comercio;
    }
}
