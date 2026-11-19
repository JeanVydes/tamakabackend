package com.tamaka.eventos.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.tamaka.eventos.dto.request.PublicarEventoRequest;
import com.tamaka.eventos.dto.response.EventoResponse;

/**
 * Casos de uso de Eventos (RF-EVE).
 */
public interface EventoService {

    EventoResponse publicar(Long gestorId, PublicarEventoRequest request);

    EventoResponse actualizar(Long eventoId, PublicarEventoRequest request);

    EventoResponse desactivar(Long eventoId);

    /** Detalle publico de un evento. */
    EventoResponse obtener(Long eventoId);

    List<EventoResponse> listarActivos(LocalDate desde);

    /** Sube una portada JPEG/PNG/WebP (max 5 MB) para el evento. */
    EventoResponse subirImagen(Long eventoId, MultipartFile archivo);

    /** Elimina la portada (y su objeto en el bucket). */
    EventoResponse eliminarImagen(Long eventoId);
}
