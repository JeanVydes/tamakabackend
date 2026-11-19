package com.tamaka.eventos.dto.response;

import java.time.LocalDate;

public record EventoResponse(
        Long id,
        String titulo,
        String descripcion,
        String lugar,
        String categoria,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        boolean activo,
        /** URL publica de la portada (R2); null si no tiene. */
        String imagen
) {
}
