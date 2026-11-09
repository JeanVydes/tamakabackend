package com.tamaka.cuentas.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tamaka.cuentas.dto.request.RegistrarCuentaRequest;
import com.tamaka.cuentas.dto.response.CuentaResponse;
import com.tamaka.cuentas.service.CuentaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/cuentas")
public class CuentaController {

    private final CuentaService cuentaService;

    public CuentaController(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    @PostMapping
    public ResponseEntity<CuentaResponse> registrar(@Valid @RequestBody RegistrarCuentaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cuentaService.registrar(request));
    }

    @GetMapping("/me")
    public ResponseEntity<CuentaResponse> miCuenta(Authentication authentication) {
        return ResponseEntity.ok(cuentaService.buscarPorId(Long.valueOf(authentication.getName())));
    }

    @PostMapping("/me/modo-operador")
    @PreAuthorize("hasRole('TURISTA')")
    public ResponseEntity<CuentaResponse> activarModoOperador(Authentication authentication) {
        return ResponseEntity.ok(cuentaService.activarModoOperador(Long.valueOf(authentication.getName())));
    }
}
