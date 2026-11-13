package com.tamaka.atractivos.service;

import java.util.List;

import com.tamaka.atractivos.dto.request.CambiarEstadoAtractivoRequest;
import com.tamaka.atractivos.dto.request.CrearAtractivoRequest;
import com.tamaka.atractivos.dto.request.EmitirAlertaRequest;
import com.tamaka.atractivos.dto.response.AtractivoDetalleResponse;
import com.tamaka.atractivos.dto.response.AtractivoResponse;

/**
 * Casos de uso de Atractivos (RF-ATT). gestorId proviene del token ya validado.
 */
public interface AtractivoService {

    AtractivoResponse crear(Long gestorId, CrearAtractivoRequest request);

    List<AtractivoResponse> buscar(String texto);

    /** Detalle publico con las alertas recientes. */
    AtractivoDetalleResponse obtener(Long atractivoId);

    AtractivoResponse cambiarEstado(Long atractivoId, CambiarEstadoAtractivoRequest request);

    void emitirAlerta(Long gestorId, EmitirAlertaRequest request);
}
