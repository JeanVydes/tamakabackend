package com.tamaka.eventos.service;

import java.time.LocalDate;
import java.util.List;

import com.tamaka.eventos.dto.request.PublicarEventoRequest;
import com.tamaka.eventos.dto.response.EventoResponse;

/**
 * Casos de uso de Eventos (RF-EVE).
 */
public interface EventoService {

    EventoResponse publicar(Long gestorId, PublicarEventoRequest request);

    EventoResponse actualizar(Long eventoId, PublicarEventoRequest request);

    EventoResponse desactivar(Long eventoId);

    List<EventoResponse> listarActivos(LocalDate desde);
}
