package com.tamaka.atractivos.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tamaka.atractivos.domain.Atractivo;
import com.tamaka.atractivos.domain.AlertaSeguridad;
import com.tamaka.atractivos.dto.request.CambiarEstadoAtractivoRequest;
import com.tamaka.atractivos.dto.request.CrearAtractivoRequest;
import com.tamaka.atractivos.dto.request.EmitirAlertaRequest;
import com.tamaka.atractivos.dto.response.AtractivoResponse;
import com.tamaka.atractivos.mapper.AtractivoMapper;
import com.tamaka.atractivos.repository.AlertaSeguridadRepository;
import com.tamaka.atractivos.repository.AtractivoRepository;
import com.tamaka.common.error.ResourceNotFoundException;
import com.tamaka.common.event.DomainEvent;
import com.tamaka.common.event.EventPublisher;

@Service
@Transactional(readOnly = true)
public class AtractivoServiceImpl implements AtractivoService {

    private final AtractivoRepository atractivoRepository;
    private final AlertaSeguridadRepository alertaRepository;
    private final AtractivoMapper atractivoMapper;
    private final EventPublisher eventPublisher;

    public AtractivoServiceImpl(AtractivoRepository atractivoRepository,
                                AlertaSeguridadRepository alertaRepository,
                                AtractivoMapper atractivoMapper,
                                EventPublisher eventPublisher) {
        this.atractivoRepository = atractivoRepository;
        this.alertaRepository = alertaRepository;
        this.atractivoMapper = atractivoMapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public AtractivoResponse crear(Long gestorId, CrearAtractivoRequest request) {
        Atractivo atractivo = Atractivo.crear(request.nombre().trim(), request.descripcion(),
                request.categoria().trim(), gestorId);
        Atractivo guardado = atractivoRepository.save(atractivo);
        eventPublisher.publicar(DomainEvent.de("AtractivoActualizado", guardado.getId(),
                Map.of("estado", guardado.getEstado().name())));
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
    @Transactional
    public AtractivoResponse cambiarEstado(Long atractivoId, CambiarEstadoAtractivoRequest request) {
        Atractivo atractivo = obtener(atractivoId);
        atractivo.cambiarEstado(request.estado());
        eventPublisher.publicar(DomainEvent.de("AtractivoActualizado", atractivoId,
                Map.of("estado", request.estado().name())));
        return atractivoMapper.toResponse(atractivo);
    }

    @Override
    @Transactional
    public void emitirAlerta(Long gestorId, EmitirAlertaRequest request) {
        Atractivo atractivo = obtener(request.atractivoId());
        alertaRepository.save(new AlertaSeguridad(atractivo.getId(), request.mensaje().trim(), gestorId));
        eventPublisher.publicar(DomainEvent.de("AlertaSeguridadEmitida", atractivo.getId(),
                Map.of("atractivoId", atractivo.getId())));
    }

    private Atractivo obtener(Long atractivoId) {
        return atractivoRepository.findById(atractivoId)
                .orElseThrow(() -> new ResourceNotFoundException("Atractivo", atractivoId));
    }
}
