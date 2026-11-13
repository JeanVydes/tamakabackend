package com.tamaka.pagos.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tamaka.pagos.dto.response.PagoDetalleResponse;
import com.tamaka.pagos.service.PagoService;

@RestController
@RequestMapping("/api/operador/pagos")
@PreAuthorize("hasRole('OPERADOR')")
public class OperadorPagoController {

    private final PagoService pagoService;

    public OperadorPagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @GetMapping
    public ResponseEntity<List<PagoDetalleResponse>> misCobros(Authentication authentication) {
        return ResponseEntity.ok(pagoService.pagosDelOperador(Long.valueOf(authentication.getName())));
    }
}
