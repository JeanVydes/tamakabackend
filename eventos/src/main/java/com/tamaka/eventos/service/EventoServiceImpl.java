package com.tamaka.eventos.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tamaka.common.error.ResourceNotFoundException;
import com.tamaka.common.event.DomainEvent;
import com.tamaka.common.event.EventPublisher;
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

    public EventoServiceImpl(EventoRepository eventoRepository,
                             EventoMapper eventoMapper,
                             EventPublisher eventPublisher) {
        this.eventoRepository = eventoRepository;
        this.eventoMapper = eventoMapper;
        this.eventPublisher = eventPublisher;
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

    private Evento obtenerEntidad(Long eventoId) {
        return eventoRepository.findById(eventoId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento", eventoId));
    }
}
