package com.tamaka.notificaciones.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.tamaka.notificaciones.domain.Seguimiento;

public interface SeguimientoRepository extends MongoRepository<Seguimiento, String> {

    List<Seguimiento> findByTipoAndRefId(String tipo, String refId);

    boolean existsByTuristaIdAndTipoAndRefId(Long turistaId, String tipo, String refId);

    void deleteByTuristaIdAndTipoAndRefId(Long turistaId, String tipo, String refId);
}
