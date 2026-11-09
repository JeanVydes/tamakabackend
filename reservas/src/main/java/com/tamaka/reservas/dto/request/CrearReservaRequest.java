package com.tamaka.reservas.dto.request;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CrearReservaRequest(

        @NotNull
        Long servicioId,

        @NotNull
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fechaReserva,

        @NotNull
        @Min(1)
        Integer numeroPersonas
) {
}
