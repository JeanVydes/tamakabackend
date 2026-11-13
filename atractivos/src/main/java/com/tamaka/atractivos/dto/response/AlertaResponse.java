package com.tamaka.atractivos.dto.response;

import java.time.Instant;

public record AlertaResponse(
        Long id,
        String mensaje,
        Instant emitidaEn
) {
}
