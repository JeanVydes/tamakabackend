package com.tamaka.common.error;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Contrato unico de error de todos los microservicios (guia de controladores §13).
 */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        Map<String, String> details
) {

    public static ErrorResponse of(int status, String error, String message, Map<String, String> details) {
        return new ErrorResponse(LocalDateTime.now(), status, error, message, details == null ? Map.of() : details);
    }
}
