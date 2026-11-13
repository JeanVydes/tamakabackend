package com.tamaka.notificaciones.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.tamaka.notificaciones.domain.EventoProcesado;

public interface EventoProcesadoRepository extends MongoRepository<EventoProcesado, String> {
}
