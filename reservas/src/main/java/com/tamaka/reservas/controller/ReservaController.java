package com.tamaka.reservas.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tamaka.reservas.dto.request.CrearReservaRequest;
import com.tamaka.reservas.dto.response.ReservaResponse;
import com.tamaka.reservas.service.ReservaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService reservaService;

    public ReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @PostMapping
    @PreAuthorize("hasRole('TURISTA')")
    public ResponseEntity<ReservaResponse> solicitar(Authentication authentication,
                                                     @Valid @RequestBody CrearReservaRequest request) {
        ReservaResponse creada = reservaService.solicitar(Long.valueOf(authentication.getName()), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    @GetMapping("/mias")
    @PreAuthorize("hasRole('TURISTA')")
    public ResponseEntity<List<ReservaResponse>> misReservas(Authentication authentication) {
        return ResponseEntity.ok(reservaService.misReservas(Long.valueOf(authentication.getName())));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('TURISTA')")
    public ResponseEntity<ReservaResponse> detalle(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(reservaService.detalleComoTurista(Long.valueOf(authentication.getName()), id));
    }

    @PatchMapping("/{id}/cancelar")
    @PreAuthorize("hasRole('TURISTA')")
    public ResponseEntity<ReservaResponse> cancelar(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(reservaService.cancelarComoTurista(Long.valueOf(authentication.getName()), id));
    }
}
