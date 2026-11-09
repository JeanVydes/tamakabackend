package com.tamaka.preferencias.service;

import java.util.List;
import java.util.Map;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.tamaka.common.error.ResourceNotFoundException;
import com.tamaka.common.event.DomainEvent;
import com.tamaka.common.event.EventPublisher;
import com.tamaka.preferencias.domain.Favorito;
import com.tamaka.preferencias.domain.Preferencia;
import com.tamaka.preferencias.dto.request.ActualizarPreferenciasRequest;
import com.tamaka.preferencias.dto.request.GuardarFavoritoRequest;
import com.tamaka.preferencias.dto.response.FavoritoResponse;
import com.tamaka.preferencias.dto.response.PreferenciasResponse;
import com.tamaka.preferencias.exception.FavoritoDuplicadoException;
import com.tamaka.preferencias.mapper.PreferenciasMapper;
import com.tamaka.preferencias.repository.FavoritoRepository;
import com.tamaka.preferencias.repository.PreferenciaRepository;

/**
 * Sin transacciones: MongoDB no requiere @Transactional para documentos individuales.
 */
@Service
public class PreferenciasServiceImpl implements PreferenciasService {

    private final PreferenciaRepository preferenciaRepository;
    private final FavoritoRepository favoritoRepository;
    private final PreferenciasMapper mapper;
    private final EventPublisher eventPublisher;

    public PreferenciasServiceImpl(PreferenciaRepository preferenciaRepository,
                                   FavoritoRepository favoritoRepository,
                                   PreferenciasMapper mapper,
                                   EventPublisher eventPublisher) {
        this.preferenciaRepository = preferenciaRepository;
        this.favoritoRepository = favoritoRepository;
        this.mapper = mapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public PreferenciasResponse actualizar(Long turistaId, ActualizarPreferenciasRequest request) {
        Preferencia preferencia = preferenciaRepository.findByTuristaId(turistaId)
                .orElseGet(() -> new Preferencia(turistaId));
        preferencia.actualizar(request.categorias(), request.idioma());
        Preferencia guardada = preferenciaRepository.save(preferencia);
        eventPublisher.publicar(DomainEvent.de("PreferenciasActualizadas", turistaId,
                Map.of("categorias", guardada.getCategorias())));
        return mapper.toResponse(guardada);
    }

    @Override
    public PreferenciasResponse obtener(Long turistaId) {
        return preferenciaRepository.findByTuristaId(turistaId)
                .map(mapper::toResponse)
                .orElseGet(() -> mapper.toResponse(new Preferencia(turistaId)));
    }

    @Override
    public FavoritoResponse guardarFavorito(Long turistaId, GuardarFavoritoRequest request) {
        if (favoritoRepository.existsByTuristaIdAndTipoAndRefId(turistaId, request.tipo(), request.refId())) {
            throw new FavoritoDuplicadoException();
        }
        Favorito guardado = favoritoRepository.save(new Favorito(turistaId, request.tipo(), request.refId()));
        eventPublisher.publicar(DomainEvent.de("FavoritoGuardado", guardado.getId(), Map.of(
                "tipo", guardado.getTipo(),
                "refId", guardado.getRefId())));
        return mapper.toResponse(guardado);
    }

    @Override
    public List<FavoritoResponse> listarFavoritos(Long turistaId) {
        return favoritoRepository.findByTuristaId(turistaId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public void eliminarFavorito(Long turistaId, String favoritoId) {
        Favorito favorito = favoritoRepository.findById(favoritoId)
                .orElseThrow(() -> new ResourceNotFoundException("Favorito", favoritoId));
        if (!turistaId.equals(favorito.getTuristaId())) {
            throw new AccessDeniedException("El favorito no pertenece al turista autenticado");
        }
        favoritoRepository.delete(favorito);
    }
}
