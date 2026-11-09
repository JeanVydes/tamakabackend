package com.tamaka.common.security;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.http.MediaType;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Escribe el contrato ErrorResponse directamente desde la capa de seguridad (401 y 403),
 * donde todavia no esta disponible el @RestControllerAdvice.
 */
final class ErrorResponseWriter {

    private ErrorResponseWriter() {
    }

    static void write(HttpServletResponse response, int status, String error, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String json = "{"
                + "\"timestamp\":\"" + LocalDateTime.now() + "\","
                + "\"status\":" + status + ","
                + "\"error\":\"" + escape(error) + "\","
                + "\"message\":\"" + escape(message) + "\","
                + "\"details\":{}"
                + "}";
        response.getWriter().write(json);
    }

    private static String escape(String texto) {
        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
