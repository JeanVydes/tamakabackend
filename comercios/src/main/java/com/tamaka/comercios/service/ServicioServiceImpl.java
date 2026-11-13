package com.tamaka.comercios.service;

import java.util.Map;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tamaka.common.error.ResourceNotFoundException;
import com.tamaka.common.event.DomainEvent;
import com.tamaka.common.event.EventPublisher;
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

    public ServicioServiceImpl(ServicioRepository servicioRepository,
                               CatalogoMapper catalogoMapper,
                               EventPublisher eventPublisher) {
        this.servicioRepository = servicioRepository;
        this.catalogoMapper = catalogoMapper;
        this.eventPublisher = eventPublisher;
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
