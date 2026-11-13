package com.tamaka.pagos.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Vista de pago para operador y administracion. Sin datos de tarjeta (la plataforma nunca los recibe).
 */
public record PagoDetalleResponse(
        Long id,
        Long reservaId,
        Long turistaId,
        Long operadorId,
        BigDecimal monto,
        String estado,
        String referenciaPasarela,
        Instant creadoEn,
        Instant aprobadoEn
) {
}
