package com.tamaka.eventos.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.tamaka.eventos.domain.Evento;
import com.tamaka.eventos.dto.response.EventoResponse;
import com.tamaka.common.storage.StorageProperties;

/**
 * Transforma Entity -> DTO. Sin reglas de negocio.
 * Construye la URL de la portada con el storage ({@link #imagenUrl(Evento)}).
 * El constructor sin argumentos deja el storage sin configurar (imagen = null, solo tests).
 */
@Component
public class EventoMapper {

    private final StorageProperties storage;

    public EventoMapper() {
        this(null);
    }

    @Autowired(required = false)
    public EventoMapper(StorageProperties storage) {
        this.storage = storage;
    }

    public EventoResponse toResponse(Evento evento) {
        return new EventoResponse(
                evento.getId(),
                evento.getTitulo(),
                evento.getDescripcion(),
                evento.getLugar(),
                evento.getCategoria(),
                evento.getFechaInicio(),
                evento.getFechaFin(),
                evento.isActivo(),
                imagenUrl(evento));
    }

    /** URL publica de la portada (R2), o null si no hay imagen o el storage no esta configurado. */
    String imagenUrl(Evento evento) {
        if (storage == null || evento.getImagenKey() == null) {
            return null;
        }
        return storage.publicBaseUrl() + "/" + evento.getImagenKey();
    }
}
