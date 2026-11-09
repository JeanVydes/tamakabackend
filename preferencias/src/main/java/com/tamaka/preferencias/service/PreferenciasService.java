package com.tamaka.preferencias.service;

import java.util.List;

import com.tamaka.preferencias.dto.request.ActualizarPreferenciasRequest;
import com.tamaka.preferencias.dto.request.GuardarFavoritoRequest;
import com.tamaka.preferencias.dto.response.FavoritoResponse;
import com.tamaka.preferencias.dto.response.PreferenciasResponse;

/**
 * Casos de uso de preferencias y favoritos del turista autenticado (RF-PRE).
 */
public interface PreferenciasService {

    PreferenciasResponse actualizar(Long turistaId, ActualizarPreferenciasRequest request);

    PreferenciasResponse obtener(Long turistaId);

    FavoritoResponse guardarFavorito(Long turistaId, GuardarFavoritoRequest request);

    List<FavoritoResponse> listarFavoritos(Long turistaId);

    void eliminarFavorito(Long turistaId, String favoritoId);
}
