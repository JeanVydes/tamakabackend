package com.tamaka.indicadores.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.tamaka.indicadores.dto.response.ConteoResponse;

/**
 * Casos de uso de Indicadores (RF-REP).
 */
public interface IndicadoresService {

    List<ConteoResponse> demanda(LocalDate desde, LocalDate hasta);

    List<ConteoResponse> indicadoresDelOperador(Long operadorId, LocalDate desde, LocalDate hasta);

    /**
     * Registro desde un evento de dominio (ReservaSolicitada, PagoAprobado, ReservaCancelada).
     * Idempotente: devuelve false si el hecho ya estaba registrado (reentrega del bus).
     */
    boolean registrarHecho(String tipo, Long operadorId, Long referenciaId, Instant ocurridoEn);
}
