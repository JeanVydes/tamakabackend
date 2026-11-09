package com.tamaka.recomendaciones.service;

import java.util.Collection;
import java.util.List;

/**
 * Casos de uso de recomendaciones. Las operaciones registrar* las invocan los consumidores de eventos.
 */
public interface RecomendacionService {

    /** Ids recomendados de un tipo ("ATRACTIVO", "SERVICIO", "EVENTO") para el turista. */
    List<Long> recomendar(String tipo, Long turistaId);

    void registrarItem(String tipo, Long itemId, String categoria);

    void registrarPreferencias(Long turistaId, Collection<String> categorias);
}
