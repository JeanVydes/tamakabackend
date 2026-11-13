package com.tamaka.comercios.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tamaka.comercios.dto.request.ActualizarServicioRequest;
import com.tamaka.comercios.dto.request.CambiarActivoRequest;
import com.tamaka.comercios.dto.response.ServicioResponse;
import com.tamaka.comercios.service.ServicioService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/servicios")
public class ServicioController {

    private final ServicioService servicioService;

    public ServicioController(ServicioService servicioService) {
        this.servicioService = servicioService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServicioResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(servicioService.obtener(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('OPERADOR')")
    public ResponseEntity<ServicioResponse> actualizar(Authentication authentication,
                                                       @PathVariable Long id,
                                                       @Valid @RequestBody ActualizarServicioRequest request) {
        return ResponseEntity.ok(servicioService.actualizar(Long.valueOf(authentication.getName()), id, request));
    }

    @PatchMapping("/{id}/activo")
    @PreAuthorize("hasRole('OPERADOR')")
    public ResponseEntity<ServicioResponse> cambiarActivo(Authentication authentication,
                                                          @PathVariable Long id,
                                                          @Valid @RequestBody CambiarActivoRequest request) {
        return ResponseEntity.ok(servicioService.cambiarActivo(Long.valueOf(authentication.getName()), id, request));
    }
}
