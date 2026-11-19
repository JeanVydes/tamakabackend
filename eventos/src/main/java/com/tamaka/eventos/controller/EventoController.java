package com.tamaka.eventos.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tamaka.eventos.dto.request.PublicarEventoRequest;
import com.tamaka.eventos.dto.response.EventoResponse;
import com.tamaka.eventos.service.EventoService;

import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/eventos")
public class EventoController {

    private final EventoService eventoService;

    public EventoController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    @GetMapping
    public ResponseEntity<List<EventoResponse>> listar(
            @RequestParam(name = "desde", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde) {
        return ResponseEntity.ok(eventoService.listarActivos(desde));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventoResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.obtener(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('GESTOR')")
    public ResponseEntity<EventoResponse> publicar(Authentication authentication,
                                                   @Valid @RequestBody PublicarEventoRequest request) {
        EventoResponse creado = eventoService.publicar(Long.valueOf(authentication.getName()), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('GESTOR')")
    public ResponseEntity<EventoResponse> actualizar(@PathVariable Long id,
                                                     @Valid @RequestBody PublicarEventoRequest request) {
        return ResponseEntity.ok(eventoService.actualizar(id, request));
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasRole('GESTOR')")
    public ResponseEntity<EventoResponse> desactivar(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.desactivar(id));
    }

    @PostMapping("/{id}/imagen")
    @PreAuthorize("hasRole('GESTOR')")
    public ResponseEntity<EventoResponse> subirImagen(@PathVariable Long id,
                                                      @RequestParam("archivo") MultipartFile archivo) {
        return ResponseEntity.ok(eventoService.subirImagen(id, archivo));
    }

    @DeleteMapping("/{id}/imagen")
    @PreAuthorize("hasRole('GESTOR')")
    public ResponseEntity<EventoResponse> eliminarImagen(@PathVariable Long id) {
        return ResponseEntity.ok(eventoService.eliminarImagen(id));
    }
}
