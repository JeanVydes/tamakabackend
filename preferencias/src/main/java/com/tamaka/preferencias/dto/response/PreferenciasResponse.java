package com.tamaka.preferencias.dto.response;

import java.util.List;

public record PreferenciasResponse(
        List<String> categorias,
        String idioma
) {
}
