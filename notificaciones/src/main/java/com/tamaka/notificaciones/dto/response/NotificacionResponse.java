package com.tamaka.notificaciones.dto.response;

import java.time.Instant;

public record NotificacionResponse(
        String id,
        String tipo,
        String titulo,
        String cuerpo,
        boolean leida,
        Instant creadaEn
) {
}
