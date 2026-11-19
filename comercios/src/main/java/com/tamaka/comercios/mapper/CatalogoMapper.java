package com.tamaka.comercios.mapper;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.tamaka.comercios.domain.Comercio;
import com.tamaka.comercios.domain.Servicio;
import com.tamaka.comercios.dto.response.ComercioDetalleResponse;
import com.tamaka.comercios.dto.response.ComercioResponse;
import com.tamaka.comercios.dto.response.ServicioResponse;
import com.tamaka.common.storage.StorageProperties;

/**
 * Transforma Entity -> DTO. Sin reglas de negocio.
 * Construye la URL de la portada con el storage ({@link #imagenUrl(Servicio)}).
 * El constructor sin argumentos deja el storage sin configurar (imagen = null, solo tests).
 */
@Component
public class CatalogoMapper {

    private final StorageProperties storage;

    public CatalogoMapper() {
        this(null);
    }

    @Autowired(required = false)
    public CatalogoMapper(StorageProperties storage) {
        this.storage = storage;
    }

    public ComercioResponse toResponse(Comercio comercio) {
        return new ComercioResponse(
                comercio.getId(),
                comercio.getOperadorId(),
                comercio.getNombre(),
                comercio.getRnt(),
                comercio.getCategoria(),
                comercio.getDescripcion(),
                comercio.isActivo(),
                imagenUrl(comercio.getImagenKey()));
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
                servicios.stream().map(this::toResponse).toList(),
                imagenUrl(comercio.getImagenKey()));
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
                servicio.getAtributos(),
                imagenUrl(servicio.getImagenKey()));
    }

    /** URL publica de la portada (R2), o null si no hay imagen o el storage no esta configurado. */
    String imagenUrl(String clave) {
        if (storage == null || clave == null) {
            return null;
        }
        return storage.publicBaseUrl() + "/" + clave;
    }
}
