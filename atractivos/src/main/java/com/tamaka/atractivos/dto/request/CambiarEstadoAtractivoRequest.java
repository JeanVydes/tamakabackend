package com.tamaka.atractivos.dto.request;

import com.tamaka.atractivos.domain.EstadoAtractivo;

import jakarta.validation.constraints.NotNull;

public record CambiarEstadoAtractivoRequest(

        @NotNull
        EstadoAtractivo estado
) {
}
