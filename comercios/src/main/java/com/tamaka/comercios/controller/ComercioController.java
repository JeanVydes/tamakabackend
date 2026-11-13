package com.tamaka.comercios.controller;

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

import com.tamaka.comercios.dto.request.ActualizarComercioRequest;
import com.tamaka.comercios.dto.request.CrearComercioRequest;
import com.tamaka.comercios.dto.request.CrearServicioRequest;
import com.tamaka.comercios.dto.response.ComercioDetalleResponse;
import com.tamaka.comercios.dto.response.ComercioResponse;
import com.tamaka.comercios.dto.response.ServicioResponse;
import com.tamaka.comercios.service.ComercioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/comercios")
public class ComercioController {

    private final ComercioService comercioService;

    public ComercioController(ComercioService comercioService) {
        this.comercioService = comercioService;
    }

    @GetMapping
    public ResponseEntity<List<ComercioResponse>> buscar(
            @RequestParam(name = "categoria", required = false) String categoria,
            @RequestParam(name = "q", required = false) String q) {
        return ResponseEntity.ok(comercioService.buscar(categoria, q));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComercioDetalleResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(comercioService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('OPERADOR')")
    public ResponseEntity<ComercioResponse> crear(Authentication authentication,
                                                  @Valid @RequestBody CrearComercioRequest request) {
        ComercioResponse creado = comercioService.crear(Long.valueOf(authentication.getName()), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('OPERADOR')")
    public ResponseEntity<ComercioResponse> actualizar(Authentication authentication,
                                                       @PathVariable Long id,
                                                       @Valid @RequestBody ActualizarComercioRequest request) {
        return ResponseEntity.ok(comercioService.actualizar(Long.valueOf(authentication.getName()), id, request));
    }

    @PostMapping("/{id}/servicios")
    @PreAuthorize("hasRole('OPERADOR')")
    public ResponseEntity<ServicioResponse> agregarServicio(Authentication authentication,
                                                            @PathVariable Long id,
                                                            @Valid @RequestBody CrearServicioRequest request) {
        ServicioResponse creado = comercioService.agregarServicio(
                Long.valueOf(authentication.getName()), id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }
}
