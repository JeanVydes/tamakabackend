package com.tamaka.cuentas.mapper;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.tamaka.cuentas.domain.Cuenta;
import com.tamaka.cuentas.dto.response.CuentaResponse;

/**
 * Transforma Entity -> DTO. Sin reglas de negocio.
 */
@Component
public class CuentaMapper {

    public CuentaResponse toResponse(Cuenta cuenta) {
        Set<String> roles = cuenta.getRoles().stream()
                .map(Enum::name)
                .collect(Collectors.toUnmodifiableSet());
        return new CuentaResponse(
                cuenta.getId(),
                cuenta.getEmail(),
                cuenta.getNombre(),
                cuenta.isActiva(),
                cuenta.isModoOperador(),
                roles,
                cuenta.getCreadaEn());
    }
}
