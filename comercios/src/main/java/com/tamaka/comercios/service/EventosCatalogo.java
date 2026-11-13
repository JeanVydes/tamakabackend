package com.tamaka.comercios.service;

import java.util.Map;

import com.tamaka.common.event.DomainEvent;
import com.tamaka.comercios.domain.Servicio;

/**
 * Construccion de los eventos de catalogo de servicios. Recomendaciones usa la categoria del comercio
 * para cruzarla con las preferencias del turista. Sin datos personales.
 */
final class EventosCatalogo {

    static final String SERVICIO_CREADO = "ServicioCreado";
    static final String SERVICIO_ACTUALIZADO = "ServicioActualizado";

    private EventosCatalogo() {
    }

    static DomainEvent servicio(String tipo, Servicio servicio) {
        return DomainEvent.de(tipo, servicio.getId(), Map.of(
                "comercioId", servicio.getComercio().getId(),
                "operadorId", servicio.getComercio().getOperadorId(),
                "categoria", servicio.getComercio().getCategoria()));
    }
}
