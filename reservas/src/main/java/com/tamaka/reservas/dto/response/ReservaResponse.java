package com.tamaka.reservas.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ReservaResponse(
        Long id,
        Long servicioId,
        Long operadorId,
        LocalDate fechaReserva,
        int numeroPersonas,
        String estado,
        BigDecimal valorAPagar,
        String observacionOperador,
        Instant creadaEn,
        Instant plazoPago
) {
}
