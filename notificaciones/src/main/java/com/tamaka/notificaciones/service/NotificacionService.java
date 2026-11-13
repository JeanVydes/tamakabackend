package com.tamaka.notificaciones.service;

import java.util.List;

import com.tamaka.notificaciones.dto.response.NotificacionResponse;

/**
 * Casos de uso de notificaciones. Los metodos de entrada (notificar*, registrar*) los invocan los consumidores
 * de eventos; las reglas de quien recibe cada aviso viven aqui, no en el consumidor.
 */
public interface NotificacionService {

    /** Crea el aviso con la plantilla en el idioma del destinatario. */
    void notificar(Long destinatarioId, String tipoEvento, String idioma);

    /**
     * Eventos de reservas y pagos: decide los destinatarios (turista y/o operador, RF-NOT-01/06/11/12)
     * y su plantilla, y usa el idioma preferido de cada uno. Idempotente por {@code eventoId}: una reentrega
     * del mismo evento no crea avisos nuevos. Devuelve cuantos avisos creo.
     */
    int notificarEvento(String eventoId, String tipoEvento, Long turistaId, Long operadorId);

    /** AtractivoActualizado / AlertaSeguridadEmitida: avisa a los seguidores del atractivo. Idempotente por eventoId. */
    int notificarSeguidoresDeAtractivo(String eventoId, String tipoEvento, Long atractivoId);

    /** Proyeccion del idioma (evento PreferenciasActualizadas). */
    void registrarIdioma(Long usuarioId, String idioma);

    /** Proyeccion de favoritos (evento FavoritoGuardado). Idempotente. */
    void registrarSeguimiento(Long turistaId, String tipo, String refId);

    /** Proyeccion de favoritos (evento FavoritoEliminado). Idempotente. */
    void eliminarSeguimiento(Long turistaId, String tipo, String refId);

    List<NotificacionResponse> misNotificaciones(Long usuarioId);

    void marcarLeida(Long usuarioId, String notificacionId);
}
