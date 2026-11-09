package com.tamaka.notificaciones.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.tamaka.notificaciones.domain.Notificacion;

public interface NotificacionRepository extends MongoRepository<Notificacion, String> {

    List<Notificacion> findByDestinatarioIdOrderByCreadaEnDesc(Long destinatarioId);

    Optional<Notificacion> findByIdAndDestinatarioId(String id, Long destinatarioId);
}
