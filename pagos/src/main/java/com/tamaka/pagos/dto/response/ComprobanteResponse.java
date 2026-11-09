package com.tamaka.pagos.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

public record ComprobanteResponse(
        Long id,
        Long reservaId,
        BigDecimal monto,
        String estado,
        String referenciaPasarela,
        Instant aprobadoEn
) {
}
