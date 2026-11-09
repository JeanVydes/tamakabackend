package com.tamaka.comercios.service;

import java.util.List;

import com.tamaka.comercios.dto.request.ActualizarComercioRequest;
import com.tamaka.comercios.dto.request.CrearComercioRequest;
import com.tamaka.comercios.dto.request.CrearServicioRequest;
import com.tamaka.comercios.dto.response.ComercioResponse;
import com.tamaka.comercios.dto.response.ServicioResponse;

/**
 * Casos de uso de catalogo de comercios (RF-CAT). operadorId proviene del token ya validado.
 */
public interface ComercioService {

    ComercioResponse crear(Long operadorId, CrearComercioRequest request);

    ComercioResponse actualizar(Long operadorId, Long comercioId, ActualizarComercioRequest request);

    ServicioResponse agregarServicio(Long operadorId, Long comercioId, CrearServicioRequest request);

    List<ComercioResponse> buscar(String categoria, String texto);
}
