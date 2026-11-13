package com.tamaka.comercios.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

import com.tamaka.comercios.domain.Servicio;

public interface ServicioRepository extends JpaRepository<Servicio, Long> {

    @EntityGraph(attributePaths = "comercio")
    @Query("select s from Servicio s where s.id = :id")
    Optional<Servicio> findConComercioPorId(@Param("id") Long id);

    /** Servicios activos de un comercio (detalle publico). */
    @Query("""
            select s from Servicio s join fetch s.comercio c
            where c.id = :comercioId and s.activo = true
            order by s.nombre
            """)
    List<Servicio> findActivosDeComercio(@Param("comercioId") Long comercioId);

    /** Servicios activos de todos los comercios de un operador (JOIN FETCH: el mapper lee el comercio). */
    @Query("""
            select s from Servicio s join fetch s.comercio c
            where c.operadorId = :operadorId and s.activo = true
            """)
    List<Servicio> findActivosDeOperador(@Param("operadorId") Long operadorId);
}
