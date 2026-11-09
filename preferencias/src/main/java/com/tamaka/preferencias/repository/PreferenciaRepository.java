package com.tamaka.preferencias.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.tamaka.preferencias.domain.Preferencia;

public interface PreferenciaRepository extends MongoRepository<Preferencia, String> {

    Optional<Preferencia> findByTuristaId(Long turistaId);
}
