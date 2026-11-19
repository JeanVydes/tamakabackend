package com.tamaka.atractivos.mapper;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.tamaka.atractivos.domain.AlertaSeguridad;
import com.tamaka.atractivos.domain.Atractivo;
import com.tamaka.atractivos.dto.response.AlertaResponse;
import com.tamaka.atractivos.dto.response.AtractivoDetalleResponse;
import com.tamaka.atractivos.dto.response.AtractivoResponse;
import com.tamaka.common.storage.StorageProperties;

/**
 * Transforma Entity -> DTO. Sin reglas de negocio.
 * Construye la URL de la portada con el storage ({@link #imagenUrl(Atractivo)}).
 * El constructor sin argumentos deja el storage sin configurar (imagen = null, solo tests).
 */
@Component
public class AtractivoMapper {

    private final StorageProperties storage;

    public AtractivoMapper() {
        this(null);
    }

    @Autowired(required = false)
    public AtractivoMapper(StorageProperties storage) {
        this.storage = storage;
    }

    public AtractivoResponse toResponse(Atractivo atractivo) {
        return new AtractivoResponse(
                atractivo.getId(),
                atractivo.getNombre(),
                atractivo.getDescripcion(),
                atractivo.getCategoria(),
                atractivo.getEstado().name(),
                imagenUrl(atractivo));
    }

    public AtractivoDetalleResponse toDetalle(Atractivo atractivo, List<AlertaSeguridad> alertas) {
        return new AtractivoDetalleResponse(
                atractivo.getId(),
                atractivo.getNombre(),
                atractivo.getDescripcion(),
                atractivo.getCategoria(),
                atractivo.getEstado().name(),
                imagenUrl(atractivo),
                alertas.stream()
                        .map(a -> new AlertaResponse(a.getId(), a.getMensaje(), a.getEmitidaEn()))
                        .toList());
    }

    /** URL publica de la portada (R2), o null si no hay imagen o el storage no esta configurado. */
    String imagenUrl(Atractivo atractivo) {
        if (storage == null || atractivo.getImagenKey() == null) {
            return null;
        }
        return storage.publicBaseUrl() + "/" + atractivo.getImagenKey();
    }
}
