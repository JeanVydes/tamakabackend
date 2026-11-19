package com.tamaka.cuentas.mapper;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.tamaka.cuentas.domain.Cuenta;
import com.tamaka.cuentas.dto.response.CuentaResponse;
import com.tamaka.common.storage.StorageProperties;

/**
 * Transforma Entity -> DTO. Sin reglas de negocio.
 * Construye la URL del avatar con el storage ({@link #imagenUrl(Cuenta)}).
 * El constructor sin argumentos deja el storage sin configurar (imagen = null, solo tests).
 */
@Component
public class CuentaMapper {

    private final StorageProperties storage;

    public CuentaMapper() {
        this(null);
    }

    @Autowired(required = false)
    public CuentaMapper(StorageProperties storage) {
        this.storage = storage;
    }

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
                cuenta.getCreadaEn(),
                imagenUrl(cuenta));
    }

    /** URL publica de la foto (R2), o null si no hay o el storage no esta configurado. */
    String imagenUrl(Cuenta cuenta) {
        if (storage == null || cuenta.getImagenKey() == null) {
            return null;
        }
        return storage.publicBaseUrl() + "/" + cuenta.getImagenKey();
    }
}
