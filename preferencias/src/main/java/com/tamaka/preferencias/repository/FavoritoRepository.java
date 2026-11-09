package com.tamaka.preferencias.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.tamaka.preferencias.domain.Favorito;

public interface FavoritoRepository extends MongoRepository<Favorito, String> {

    List<Favorito> findByTuristaId(Long turistaId);

    Optional<Favorito> findByIdAndTuristaId(String id, Long turistaId);

    boolean existsByTuristaIdAndTipoAndRefId(Long turistaId, String tipo, String refId);
}
