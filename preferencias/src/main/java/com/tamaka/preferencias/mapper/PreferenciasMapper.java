package com.tamaka.preferencias.mapper;

import org.springframework.stereotype.Component;

import com.tamaka.preferencias.domain.Favorito;
import com.tamaka.preferencias.domain.Preferencia;
import com.tamaka.preferencias.dto.response.FavoritoResponse;
import com.tamaka.preferencias.dto.response.PreferenciasResponse;

/**
 * Transforma documentos -> DTO. Sin reglas de negocio.
 */
@Component
public class PreferenciasMapper {

    public PreferenciasResponse toResponse(Preferencia preferencia) {
        return new PreferenciasResponse(preferencia.getCategorias(), preferencia.getIdioma());
    }

    public FavoritoResponse toResponse(Favorito favorito) {
        return new FavoritoResponse(favorito.getId(), favorito.getTipo(), favorito.getRefId());
    }
}
