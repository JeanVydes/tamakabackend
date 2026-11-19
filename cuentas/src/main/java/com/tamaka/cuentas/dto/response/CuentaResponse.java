package com.tamaka.cuentas.dto.response;

import java.time.Instant;
import java.util.Set;

public record CuentaResponse(
        Long id,
        String email,
        String nombre,
        boolean activa,
        boolean modoOperador,
        Set<String> roles,
        Instant creadaEn,
        /** URL publica de la foto de perfil (R2); null si usa iniciales. */
        String imagen
) {
}
