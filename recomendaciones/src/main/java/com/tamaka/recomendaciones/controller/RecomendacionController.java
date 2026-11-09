package com.tamaka.recomendaciones.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tamaka.recomendaciones.service.RecomendacionService;

@RestController
@RequestMapping("/api/recomendaciones")
@PreAuthorize("hasRole('TURISTA')")
public class RecomendacionController {

    private final RecomendacionService recomendacionService;

    public RecomendacionController(RecomendacionService recomendacionService) {
        this.recomendacionService = recomendacionService;
    }

    @GetMapping("/atractivos")
    public ResponseEntity<List<Long>> atractivos(Authentication authentication) {
        return ResponseEntity.ok(recomendacionService.recomendar("ATRACTIVO", turista(authentication)));
    }

    @GetMapping("/servicios")
    public ResponseEntity<List<Long>> servicios(Authentication authentication) {
        return ResponseEntity.ok(recomendacionService.recomendar("SERVICIO", turista(authentication)));
    }

    @GetMapping("/eventos")
    public ResponseEntity<List<Long>> eventos(Authentication authentication) {
        return ResponseEntity.ok(recomendacionService.recomendar("EVENTO", turista(authentication)));
    }

    private static Long turista(Authentication authentication) {
        return Long.valueOf(authentication.getName());
    }
}
