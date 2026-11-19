package com.tamaka.eventos.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.tamaka.common.error.ResourceNotFoundException;
import com.tamaka.common.event.DomainEvent;
import com.tamaka.common.event.EventPublisher;
import com.tamaka.common.storage.ImageStorage;
import com.tamaka.eventos.domain.Evento;
import com.tamaka.eventos.dto.request.PublicarEventoRequest;
import com.tamaka.eventos.dto.response.EventoResponse;
import com.tamaka.eventos.mapper.EventoMapper;
import com.tamaka.eventos.repository.EventoRepository;

@Service
@Transactional(readOnly = true)
public class EventoServiceImpl implements EventoService {

    private final EventoRepository eventoRepository;
    private final EventoMapper eventoMapper;
    private final EventPublisher eventPublisher;
    private final ImageStorage imageStorage;

    public EventoServiceImpl(EventoRepository eventoRepository,
                             EventoMapper eventoMapper,
                             EventPublisher eventPublisher,
                             ImageStorage imageStorage) {
        this.eventoRepository = eventoRepository;
        this.eventoMapper = eventoMapper;
        this.eventPublisher = eventPublisher;
        this.imageStorage = imageStorage;
    }

    @Override
    @Transactional
    public EventoResponse publicar(Long gestorId, PublicarEventoRequest request) {
        Evento evento = Evento.publicar(request.titulo().trim(), request.descripcion(), request.lugar().trim(),
                request.categoria(), request.fechaInicio(), request.fechaFin(), gestorId);
        Evento guardado = eventoRepository.save(evento);
        eventPublisher.publicar(DomainEvent.de("EventoPublicado", guardado.getId(), Map.of(
                "categoria", guardado.getCategoria(),
                "fechaInicio", guardado.getFechaInicio().toString(),
                "fechaFin", guardado.getFechaFin().toString())));
        return eventoMapper.toResponse(guardado);
    }

    @Override
    @Transactional
    public EventoResponse actualizar(Long eventoId, PublicarEventoRequest request) {
        Evento evento = obtenerEntidad(eventoId);
        evento.actualizar(request.titulo().trim(), request.descripcion(), request.lugar().trim(),
                request.categoria(), request.fechaInicio(), request.fechaFin());
        return eventoMapper.toResponse(evento);
    }

    @Override
    @Transactional
    public EventoResponse desactivar(Long eventoId) {
        Evento evento = obtenerEntidad(eventoId);
        if (evento.isActivo()) {
            evento.desactivar();
            eventPublisher.publicar(DomainEvent.de("EventoDesactivado", eventoId, Map.of()));
        }
        return eventoMapper.toResponse(evento);
    }

    @Override
    public EventoResponse obtener(Long eventoId) {
        return eventoMapper.toResponse(obtenerEntidad(eventoId));
    }

    @Override
    public List<EventoResponse> listarActivos(LocalDate desde) {
        LocalDate inicio = desde == null ? LocalDate.now() : desde;
        return eventoRepository.findActivosDesde(inicio).stream()
                .map(eventoMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public EventoResponse subirImagen(Long eventoId, MultipartFile archivo) {
        Evento evento = obtenerEntidad(eventoId);
        // subir valida tipo/firma/5MB y devuelve la clave del objeto ya guardado en R2
        String claveNueva = imageStorage.subir("eventos", String.valueOf(evento.getId()), archivo);
        String vieja = evento.getImagenKey();
        evento.cambiarImagen(claveNueva);
        eventoRepository.save(evento);
        imageStorage.borrar(vieja);
        return eventoMapper.toResponse(evento);
    }

    @Override
    @Transactional
    public EventoResponse eliminarImagen(Long eventoId) {
        Evento evento = obtenerEntidad(eventoId);
        String vieja = evento.getImagenKey();
        evento.cambiarImagen(null);
        eventoRepository.save(evento);
        imageStorage.borrar(vieja);
        return eventoMapper.toResponse(evento);
    }

    private Evento obtenerEntidad(Long eventoId) {
        return eventoRepository.findById(eventoId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento", eventoId));
    }
}
