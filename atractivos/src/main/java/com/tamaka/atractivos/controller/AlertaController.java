package com.tamaka.atractivos.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tamaka.atractivos.dto.request.EmitirAlertaRequest;
import com.tamaka.atractivos.service.AtractivoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/alertas")
public class AlertaController {

    private final AtractivoService atractivoService;

    public AlertaController(AtractivoService atractivoService) {
        this.atractivoService = atractivoService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('GESTOR', 'ADMIN')")
    public ResponseEntity<Void> emitir(Authentication authentication,
                                       @Valid @RequestBody EmitirAlertaRequest request) {
        atractivoService.emitirAlerta(Long.valueOf(authentication.getName()), request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
