package com.tamaka.pagos.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tamaka.pagos.domain.EstadoPago;
import com.tamaka.pagos.dto.response.PagoDetalleResponse;
import com.tamaka.pagos.service.PagoService;

@RestController
@RequestMapping("/api/admin/pagos")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPagoController {

    private final PagoService pagoService;

    public AdminPagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @GetMapping
    public ResponseEntity<List<PagoDetalleResponse>> listar(
            @RequestParam(name = "estado", required = false) EstadoPago estado) {
        return ResponseEntity.ok(pagoService.listarTodos(estado));
    }
}
