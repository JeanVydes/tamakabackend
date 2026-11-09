package com.tamaka.pagos.dto.response;

import java.math.BigDecimal;

/**
 * @param urlPago URL de la pasarela donde el turista completa el pago (solo al iniciar).
 */
public record PagoResponse(
        Long id,
        Long reservaId,
        BigDecimal monto,
        String estado,
        String referenciaPasarela,
        String urlPago
) {
}
