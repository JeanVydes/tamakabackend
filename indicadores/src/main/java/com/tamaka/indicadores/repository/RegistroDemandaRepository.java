package com.tamaka.indicadores.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tamaka.indicadores.domain.RegistroDemanda;

public interface RegistroDemandaRepository extends JpaRepository<RegistroDemanda, Long> {

    /** Proyeccion: conteo por tipo en un rango de dias. */
    interface ConteoPorTipo {
        String getTipo();

        long getTotal();
    }

    @Query("""
            select r.tipo as tipo, count(r) as total
            from RegistroDemanda r
            where r.dia between :desde and :hasta
            group by r.tipo
            order by r.tipo
            """)
    List<ConteoPorTipo> contarPorTipo(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("""
            select r.tipo as tipo, count(r) as total
            from RegistroDemanda r
            where r.operadorId = :operadorId
              and r.dia between :desde and :hasta
            group by r.tipo
            order by r.tipo
            """)
    List<ConteoPorTipo> contarPorTipoDeOperador(@Param("operadorId") Long operadorId,
                                                @Param("desde") LocalDate desde,
                                                @Param("hasta") LocalDate hasta);
}
