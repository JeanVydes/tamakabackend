package com.tamaka.eventos.dto.response;

import java.time.LocalDate;

public record EventoResponse(
        Long id,
        String titulo,
        String descripcion,
        String lugar,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        boolean activo
) {
}
