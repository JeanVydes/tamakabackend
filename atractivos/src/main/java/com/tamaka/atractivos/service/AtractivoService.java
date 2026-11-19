package com.tamaka.atractivos.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

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

    /** Sube una portada JPEG/PNG/WebP (max 5 MB) para el atractivo. */
    AtractivoResponse subirImagen(Long atractivoId, MultipartFile archivo);

    /** Elimina la portada (y su objeto en el bucket). */
    AtractivoResponse eliminarImagen(Long atractivoId);
}
