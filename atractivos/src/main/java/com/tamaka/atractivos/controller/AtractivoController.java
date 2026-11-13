package com.tamaka.atractivos.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tamaka.atractivos.dto.request.CambiarEstadoAtractivoRequest;
import com.tamaka.atractivos.dto.request.CrearAtractivoRequest;
import com.tamaka.atractivos.dto.response.AtractivoDetalleResponse;
import com.tamaka.atractivos.dto.response.AtractivoResponse;
import com.tamaka.atractivos.service.AtractivoService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/atractivos")
public class AtractivoController {

    private final AtractivoService atractivoService;

    public AtractivoController(AtractivoService atractivoService) {
        this.atractivoService = atractivoService;
    }

    @GetMapping
    public ResponseEntity<List<AtractivoResponse>> buscar(
            @RequestParam(name = "q", required = false) String q) {
        return ResponseEntity.ok(atractivoService.buscar(q));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AtractivoDetalleResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(atractivoService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('GESTOR', 'ADMIN')")
    public ResponseEntity<AtractivoResponse> crear(Authentication authentication,
                                                   @Valid @RequestBody CrearAtractivoRequest request) {
        AtractivoResponse creado = atractivoService.crear(Long.valueOf(authentication.getName()), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAnyRole('GESTOR', 'ADMIN')")
    public ResponseEntity<AtractivoResponse> cambiarEstado(@PathVariable Long id,
                                                           @Valid @RequestBody CambiarEstadoAtractivoRequest request) {
        return ResponseEntity.ok(atractivoService.cambiarEstado(id, request));
    }
}
