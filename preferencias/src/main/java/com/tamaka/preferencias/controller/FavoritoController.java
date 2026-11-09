package com.tamaka.preferencias.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tamaka.preferencias.dto.request.GuardarFavoritoRequest;
import com.tamaka.preferencias.dto.response.FavoritoResponse;
import com.tamaka.preferencias.service.PreferenciasService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/favoritos")
@PreAuthorize("hasRole('TURISTA')")
public class FavoritoController {

    private final PreferenciasService preferenciasService;

    public FavoritoController(PreferenciasService preferenciasService) {
        this.preferenciasService = preferenciasService;
    }

    @PostMapping
    public ResponseEntity<FavoritoResponse> guardar(Authentication authentication,
                                                    @Valid @RequestBody GuardarFavoritoRequest request) {
        FavoritoResponse guardado = preferenciasService.guardarFavorito(Long.valueOf(authentication.getName()), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(guardado);
    }

    @GetMapping
    public ResponseEntity<List<FavoritoResponse>> listar(Authentication authentication) {
        return ResponseEntity.ok(preferenciasService.listarFavoritos(Long.valueOf(authentication.getName())));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(Authentication authentication, @PathVariable String id) {
        preferenciasService.eliminarFavorito(Long.valueOf(authentication.getName()), id);
        return ResponseEntity.noContent().build();
    }
}
