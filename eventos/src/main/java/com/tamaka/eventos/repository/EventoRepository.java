package com.tamaka.eventos.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tamaka.eventos.domain.Evento;

public interface EventoRepository extends JpaRepository<Evento, Long> {

    /** Eventos activos que aun no terminaron, opcionalmente desde una fecha. */
    @Query("""
            select e from Evento e
            where e.activo = true
              and e.fechaFin >= :desde
            order by e.fechaInicio
            """)
    List<Evento> findActivosDesde(@Param("desde") LocalDate desde);
}
