package com.tamaka.reservas.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tamaka.reservas.domain.EstadoReserva;
import com.tamaka.reservas.dto.response.ReservaResponse;
import com.tamaka.reservas.service.ReservaService;

@RestController
@RequestMapping("/api/admin/reservas")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReservaController {

    private final ReservaService reservaService;

    public AdminReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @GetMapping
    public ResponseEntity<List<ReservaResponse>> listar(
            @RequestParam(name = "estado", required = false) EstadoReserva estado) {
        return ResponseEntity.ok(reservaService.listarTodas(estado));
    }
}
