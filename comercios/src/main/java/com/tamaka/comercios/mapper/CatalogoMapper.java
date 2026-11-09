package com.tamaka.comercios.mapper;

import org.springframework.stereotype.Component;

import com.tamaka.comercios.domain.Comercio;
import com.tamaka.comercios.domain.Servicio;
import com.tamaka.comercios.dto.response.ComercioResponse;
import com.tamaka.comercios.dto.response.ServicioResponse;

/**
 * Transforma Entity -> DTO. Sin reglas de negocio.
 */
@Component
public class CatalogoMapper {

    public ComercioResponse toResponse(Comercio comercio) {
        return new ComercioResponse(
                comercio.getId(),
                comercio.getOperadorId(),
                comercio.getNombre(),
                comercio.getRnt(),
                comercio.getCategoria(),
                comercio.getDescripcion(),
                comercio.isActivo());
    }

    public ServicioResponse toResponse(Servicio servicio) {
        return new ServicioResponse(
                servicio.getId(),
                servicio.getComercio().getId(),
                servicio.getComercio().getOperadorId(),
                servicio.getNombre(),
                servicio.getPrecioBase(),
                servicio.getMinPersonas(),
                servicio.getMaxPersonas(),
                servicio.isActivo(),
                servicio.getAtributos());
    }
}
