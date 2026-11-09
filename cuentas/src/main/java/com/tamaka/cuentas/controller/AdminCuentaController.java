package com.tamaka.cuentas.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tamaka.cuentas.dto.request.SuspensionRequest;
import com.tamaka.cuentas.dto.response.CuentaResponse;
import com.tamaka.cuentas.service.CuentaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/cuentas")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCuentaController {

    private final CuentaService cuentaService;

    public AdminCuentaController(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    @PatchMapping("/{id}/suspension")
    public ResponseEntity<CuentaResponse> cambiarSuspension(@PathVariable Long id,
                                                            @Valid @RequestBody SuspensionRequest request) {
        return ResponseEntity.ok(cuentaService.cambiarSuspension(id, request));
    }

    @PutMapping("/{id}/roles/gestor")
    public ResponseEntity<CuentaResponse> asignarGestor(@PathVariable Long id) {
        return ResponseEntity.ok(cuentaService.asignarRolGestor(id));
    }
}
