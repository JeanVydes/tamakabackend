package com.tamaka.atractivos.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tamaka.atractivos.domain.Atractivo;

public interface AtractivoRepository extends JpaRepository<Atractivo, Long> {

    /** Busqueda publica. Un texto vacio significa "sin filtro" (evita parametros nulos en PostgreSQL). */
    @Query("""
            select a from Atractivo a
            where (:texto = '' or lower(a.nombre) like lower(concat('%', :texto, '%')))
            order by a.nombre
            """)
    List<Atractivo> buscar(@Param("texto") String texto);
}
