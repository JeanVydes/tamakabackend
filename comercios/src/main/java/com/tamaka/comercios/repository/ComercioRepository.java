package com.tamaka.comercios.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tamaka.comercios.domain.Comercio;

public interface ComercioRepository extends JpaRepository<Comercio, Long> {

    boolean existsByRnt(String rnt);

    List<Comercio> findByOperadorId(Long operadorId);

    /**
     * Busqueda del catalogo publico. Los parametros vacios significan "sin filtro"
     * (evita parametros nulos en PostgreSQL).
     */
    @Query("""
            select c from Comercio c
            where c.activo = true
              and (:categoria = '' or c.categoria = :categoria)
              and (:texto = '' or lower(c.nombre) like lower(concat('%', :texto, '%')))
            order by c.nombre
            """)
    List<Comercio> buscarActivos(@Param("categoria") String categoria, @Param("texto") String texto);
}
