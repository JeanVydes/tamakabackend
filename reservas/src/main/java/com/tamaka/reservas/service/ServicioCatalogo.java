package com.tamaka.reservas.service;

/**
 * Puerto de salida hacia Comercios: consulta puntual (no se leen tablas ajenas).
 * El adaptador HTTP vive en la capa de persistencia (repository).
 */
public interface ServicioCatalogo {

    ServicioRef obtener(Long servicioId);

    /** Datos del servicio necesarios para validar la reserva y la pertenencia del operador. */
    record ServicioRef(Long id, Long operadorId, int minPersonas, int maxPersonas, boolean activo) {
    }
}
