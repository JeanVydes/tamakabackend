package com.tamaka.notificaciones.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tamaka.notificaciones.dto.response.NotificacionResponse;
import com.tamaka.notificaciones.service.NotificacionService;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @GetMapping
    public ResponseEntity<List<NotificacionResponse>> listar(Authentication authentication) {
        return ResponseEntity.ok(notificacionService.misNotificaciones(Long.valueOf(authentication.getName())));
    }

    @PatchMapping("/{id}/leida")
    public ResponseEntity<Void> marcarLeida(Authentication authentication, @PathVariable String id) {
        notificacionService.marcarLeida(Long.valueOf(authentication.getName()), id);
        return ResponseEntity.ok().build();
    }
}
