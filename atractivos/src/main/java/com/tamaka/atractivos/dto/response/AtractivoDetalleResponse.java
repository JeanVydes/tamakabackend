package com.tamaka.atractivos.dto.response;

import java.util.List;

/**
 * Detalle publico de un atractivo con sus alertas de seguridad mas recientes.
 */
public record AtractivoDetalleResponse(
        Long id,
        String nombre,
        String descripcion,
        String categoria,
        String estado,
        /** URL publica de la portada (R2); null si no tiene. */
        String imagen,
        List<AlertaResponse> alertasRecientes
) {
}
