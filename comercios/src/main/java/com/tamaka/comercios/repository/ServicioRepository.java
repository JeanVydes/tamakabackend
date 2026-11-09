package com.tamaka.comercios.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

import com.tamaka.comercios.domain.Servicio;

public interface ServicioRepository extends JpaRepository<Servicio, Long> {

    @EntityGraph(attributePaths = "comercio")
    @Query("select s from Servicio s where s.id = :id")
    Optional<Servicio> findConComercioPorId(@Param("id") Long id);
}
