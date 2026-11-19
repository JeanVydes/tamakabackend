package com.tamaka.comercios.dto.response;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Vista publica del servicio. operadorId permite a Reservas comprobar la pertenencia (consulta HTTP puntual).
 */
public record ServicioResponse(
        Long id,
        Long comercioId,
        Long operadorId,
        String nombre,
        BigDecimal precioBase,
        int minPersonas,
        int maxPersonas,
        boolean activo,
        Map<String, Object> atributos,
        /** URL publica de la portada (R2); null si no tiene. */
        String imagen
) {
}
