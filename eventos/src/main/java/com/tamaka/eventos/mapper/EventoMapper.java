package com.tamaka.eventos.mapper;

import org.springframework.stereotype.Component;

import com.tamaka.eventos.domain.Evento;
import com.tamaka.eventos.dto.response.EventoResponse;

/**
 * Transforma Entity -> DTO. Sin reglas de negocio.
 */
@Component
public class EventoMapper {

    public EventoResponse toResponse(Evento evento) {
        return new EventoResponse(
                evento.getId(),
                evento.getTitulo(),
                evento.getDescripcion(),
                evento.getLugar(),
                evento.getFechaInicio(),
                evento.getFechaFin(),
                evento.isActivo());
    }
}
