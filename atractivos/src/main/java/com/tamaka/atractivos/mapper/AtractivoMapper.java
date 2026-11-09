package com.tamaka.atractivos.mapper;

import org.springframework.stereotype.Component;

import com.tamaka.atractivos.domain.Atractivo;
import com.tamaka.atractivos.dto.response.AtractivoResponse;

/**
 * Transforma Entity -> DTO. Sin reglas de negocio.
 */
@Component
public class AtractivoMapper {

    public AtractivoResponse toResponse(Atractivo atractivo) {
        return new AtractivoResponse(
                atractivo.getId(),
                atractivo.getNombre(),
                atractivo.getDescripcion(),
                atractivo.getCategoria(),
                atractivo.getEstado().name());
    }
}
