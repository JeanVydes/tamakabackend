package com.tamaka.preferencias.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ActualizarPreferenciasRequest(

        @NotNull
        @Size(max = 20)
        List<@NotBlank @Size(max = 60) String> categorias,

        @NotNull
        @Pattern(regexp = "es|en", message = "debe ser es o en")
        String idioma
) {
}
