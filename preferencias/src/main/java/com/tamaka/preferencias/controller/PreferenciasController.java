package com.tamaka.preferencias.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tamaka.preferencias.dto.request.ActualizarPreferenciasRequest;
import com.tamaka.preferencias.dto.response.PreferenciasResponse;
import com.tamaka.preferencias.service.PreferenciasService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/preferencias/me")
@PreAuthorize("hasRole('TURISTA')")
public class PreferenciasController {

    private final PreferenciasService preferenciasService;

    public PreferenciasController(PreferenciasService preferenciasService) {
        this.preferenciasService = preferenciasService;
    }

    @GetMapping
    public ResponseEntity<PreferenciasResponse> obtener(Authentication authentication) {
        return ResponseEntity.ok(preferenciasService.obtener(Long.valueOf(authentication.getName())));
    }

    @PutMapping
    public ResponseEntity<PreferenciasResponse> actualizar(Authentication authentication,
                                                           @Valid @RequestBody ActualizarPreferenciasRequest request) {
        return ResponseEntity.ok(preferenciasService.actualizar(Long.valueOf(authentication.getName()), request));
    }
}
