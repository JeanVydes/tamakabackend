package com.tamaka.indicadores.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tamaka.indicadores.dto.response.ConteoResponse;
import com.tamaka.indicadores.service.IndicadoresService;

/**
 * Dos vistas: demanda global (GESTOR, ADMIN) y, para el operador, solo sobre sus propios servicios.
 */
@RestController
public class IndicadorController {

    private static final int DIAS_OPERADOR_POR_DEFECTO = 30;

    private final IndicadoresService indicadoresService;

    public IndicadorController(IndicadoresService indicadoresService) {
        this.indicadoresService = indicadoresService;
    }

    @GetMapping("/api/indicadores/demanda")
    @PreAuthorize("hasAnyRole('GESTOR', 'ADMIN')")
    public ResponseEntity<List<ConteoResponse>> demanda(
            @RequestParam("desde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam("hasta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(indicadoresService.demanda(desde, hasta));
    }

    @GetMapping("/api/operador/indicadores")
    @PreAuthorize("hasRole('OPERADOR')")
    public ResponseEntity<List<ConteoResponse>> indicadoresOperador(Authentication authentication) {
        LocalDate hasta = LocalDate.now();
        LocalDate desde = hasta.minusDays(DIAS_OPERADOR_POR_DEFECTO);
        return ResponseEntity.ok(indicadoresService.indicadoresDelOperador(
                Long.valueOf(authentication.getName()), desde, hasta));
    }
}
