package com.tamaka.reservas.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tamaka.reservas.dto.request.CancelarReservaRequest;
import com.tamaka.reservas.dto.request.ResponderSolicitudRequest;
import com.tamaka.reservas.dto.response.ReservaResponse;
import com.tamaka.reservas.service.ReservaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/operador/reservas")
@PreAuthorize("hasRole('OPERADOR')")
public class OperadorReservaController {

    private final ReservaService reservaService;

    public OperadorReservaController(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @GetMapping
    public ResponseEntity<List<ReservaResponse>> listar(Authentication authentication) {
        return ResponseEntity.ok(reservaService.reservasDelOperador(Long.valueOf(authentication.getName())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservaResponse> detalle(Authentication authentication, @PathVariable Long id) {
        return ResponseEntity.ok(reservaService.detalleComoOperador(Long.valueOf(authentication.getName()), id));
    }

    @PatchMapping("/{id}/respuesta")
    public ResponseEntity<ReservaResponse> responder(Authentication authentication,
                                                     @PathVariable Long id,
                                                     @Valid @RequestBody ResponderSolicitudRequest request) {
        return ResponseEntity.ok(reservaService.responder(Long.valueOf(authentication.getName()), id, request));
    }

    /** Cuerpo opcional: {"motivo": "..."}. */
    @PatchMapping("/{id}/cancelar")
    public ResponseEntity<ReservaResponse> cancelar(Authentication authentication,
                                                    @PathVariable Long id,
                                                    @Valid @RequestBody(required = false) CancelarReservaRequest request) {
        return ResponseEntity.ok(reservaService.cancelarComoOperador(Long.valueOf(authentication.getName()), id, request));
    }
}
