package com.tamaka.comercios.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.tamaka.comercios.domain.Comercio;
import com.tamaka.comercios.domain.Servicio;
import com.tamaka.comercios.dto.response.ComercioDetalleResponse;
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

    public ComercioDetalleResponse toDetalle(Comercio comercio, List<Servicio> servicios) {
        return new ComercioDetalleResponse(
                comercio.getId(),
                comercio.getOperadorId(),
                comercio.getNombre(),
                comercio.getRnt(),
                comercio.getCategoria(),
                comercio.getDescripcion(),
                comercio.isActivo(),
                servicios.stream().map(this::toResponse).toList());
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
