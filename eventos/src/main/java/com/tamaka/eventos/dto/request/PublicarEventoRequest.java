package com.tamaka.eventos.dto.request;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PublicarEventoRequest(

        @NotBlank
        @Size(max = 150)
        String titulo,

        @Size(max = 2000)
        String descripcion,

        @NotBlank
        @Size(max = 200)
        String lugar,

        @NotNull
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fechaInicio,

        @NotNull
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fechaFin
) {
}
