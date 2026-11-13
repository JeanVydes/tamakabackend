package com.tamaka.atractivos.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.tamaka.atractivos.domain.AlertaSeguridad;
import com.tamaka.atractivos.domain.Atractivo;
import com.tamaka.atractivos.dto.response.AlertaResponse;
import com.tamaka.atractivos.dto.response.AtractivoDetalleResponse;
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

    public AtractivoDetalleResponse toDetalle(Atractivo atractivo, List<AlertaSeguridad> alertas) {
        return new AtractivoDetalleResponse(
                atractivo.getId(),
                atractivo.getNombre(),
                atractivo.getDescripcion(),
                atractivo.getCategoria(),
                atractivo.getEstado().name(),
                alertas.stream()
                        .map(a -> new AlertaResponse(a.getId(), a.getMensaje(), a.getEmitidaEn()))
                        .toList());
    }
}
