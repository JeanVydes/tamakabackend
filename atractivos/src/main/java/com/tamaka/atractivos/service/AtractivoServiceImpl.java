package com.tamaka.atractivos.service;

import java.util.List;
import java.util.Map;

import org.springframework.web.multipart.MultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tamaka.atractivos.domain.Atractivo;
import com.tamaka.atractivos.domain.AlertaSeguridad;
import com.tamaka.atractivos.dto.request.CambiarEstadoAtractivoRequest;
import com.tamaka.atractivos.dto.request.CrearAtractivoRequest;
import com.tamaka.atractivos.dto.request.EmitirAlertaRequest;
import com.tamaka.atractivos.dto.response.AtractivoDetalleResponse;
import com.tamaka.atractivos.dto.response.AtractivoResponse;
import com.tamaka.atractivos.mapper.AtractivoMapper;
import com.tamaka.atractivos.repository.AlertaSeguridadRepository;
import com.tamaka.atractivos.repository.AtractivoRepository;
import com.tamaka.common.error.ResourceNotFoundException;
import com.tamaka.common.event.DomainEvent;
import com.tamaka.common.event.EventPublisher;
import com.tamaka.common.storage.ImageStorage;

@Service
@Transactional(readOnly = true)
public class AtractivoServiceImpl implements AtractivoService {

    private final AtractivoRepository atractivoRepository;
    private final AlertaSeguridadRepository alertaRepository;
    private final AtractivoMapper atractivoMapper;
    private final EventPublisher eventPublisher;
    private final ImageStorage imageStorage;

    public AtractivoServiceImpl(AtractivoRepository atractivoRepository,
                                AlertaSeguridadRepository alertaRepository,
                                AtractivoMapper atractivoMapper,
                                EventPublisher eventPublisher,
                                ImageStorage imageStorage) {
        this.atractivoRepository = atractivoRepository;
        this.alertaRepository = alertaRepository;
        this.atractivoMapper = atractivoMapper;
        this.eventPublisher = eventPublisher;
        this.imageStorage = imageStorage;
    }

    @Override
    @Transactional
    public AtractivoResponse crear(Long gestorId, CrearAtractivoRequest request) {
        Atractivo atractivo = Atractivo.crear(request.nombre().trim(), request.descripcion(),
                request.categoria().trim(), gestorId);
        Atractivo guardado = atractivoRepository.save(atractivo);
        publicarActualizado(guardado);
        return atractivoMapper.toResponse(guardado);
    }

    @Override
    public List<AtractivoResponse> buscar(String texto) {
        String filtro = texto == null ? "" : texto.trim();
        return atractivoRepository.buscar(filtro).stream()
                .map(atractivoMapper::toResponse)
                .toList();
    }

    @Override
    public AtractivoDetalleResponse obtener(Long atractivoId) {
        Atractivo atractivo = obtenerEntidad(atractivoId);
        return atractivoMapper.toDetalle(atractivo,
                alertaRepository.findTop5ByAtractivoIdOrderByEmitidaEnDesc(atractivoId));
    }

    @Override
    @Transactional
    public AtractivoResponse cambiarEstado(Long atractivoId, CambiarEstadoAtractivoRequest request) {
        Atractivo atractivo = obtenerEntidad(atractivoId);
        atractivo.cambiarEstado(request.estado());
        publicarActualizado(atractivo);
        return atractivoMapper.toResponse(atractivo);
    }

    @Override
    @Transactional
    public AtractivoResponse subirImagen(Long atractivoId, MultipartFile archivo) {
        Atractivo atractivo = obtenerEntidad(atractivoId);
        // subir valida tipo/firma/5MB y devuelve la clave del objeto ya guardado en R2
        String claveNueva = imageStorage.subir("atractivos", String.valueOf(atractivo.getId()), archivo);
        String vieja = atractivo.getImagenKey();
        atractivo.cambiarImagen(claveNueva);
        atractivoRepository.save(atractivo);
        imageStorage.borrar(vieja);
        return atractivoMapper.toResponse(atractivo);
    }

    @Override
    @Transactional
    public AtractivoResponse eliminarImagen(Long atractivoId) {
        Atractivo atractivo = obtenerEntidad(atractivoId);
        String vieja = atractivo.getImagenKey();
        atractivo.cambiarImagen(null);
        atractivoRepository.save(atractivo);
        imageStorage.borrar(vieja);
        return atractivoMapper.toResponse(atractivo);
    }

    @Override
    @Transactional
    public void emitirAlerta(Long gestorId, EmitirAlertaRequest request) {
        Atractivo atractivo = obtenerEntidad(request.atractivoId());
        alertaRepository.save(new AlertaSeguridad(atractivo.getId(), request.mensaje().trim(), gestorId));
        eventPublisher.publicar(DomainEvent.de("AlertaSeguridadEmitida", atractivo.getId(),
                Map.of("atractivoId", atractivo.getId())));
    }

    /** Recomendaciones necesita la categoria; Notificaciones avisa a quienes siguen el atractivo. */
    private void publicarActualizado(Atractivo atractivo) {
        eventPublisher.publicar(DomainEvent.de("AtractivoActualizado", atractivo.getId(), Map.of(
                "estado", atractivo.getEstado().name(),
                "categoria", atractivo.getCategoria())));
    }

    private Atractivo obtenerEntidad(Long atractivoId) {
        return atractivoRepository.findById(atractivoId)
                .orElseThrow(() -> new ResourceNotFoundException("Atractivo", atractivoId));
    }
}
