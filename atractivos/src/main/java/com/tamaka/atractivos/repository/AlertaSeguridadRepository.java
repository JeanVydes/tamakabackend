package com.tamaka.atractivos.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tamaka.atractivos.domain.AlertaSeguridad;

public interface AlertaSeguridadRepository extends JpaRepository<AlertaSeguridad, Long> {

    /** Alertas mas recientes de un atractivo (detalle publico). */
    List<AlertaSeguridad> findTop5ByAtractivoIdOrderByEmitidaEnDesc(Long atractivoId);
}
