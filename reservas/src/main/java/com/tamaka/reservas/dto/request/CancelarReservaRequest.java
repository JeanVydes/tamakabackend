package com.tamaka.reservas.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Cancelacion por parte del operador. El motivo queda en el historial de auditoria.
 */
public record CancelarReservaRequest(

        @Size(max = 300)
        String motivo
) {
}
