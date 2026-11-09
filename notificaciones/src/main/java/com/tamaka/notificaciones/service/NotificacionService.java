package com.tamaka.notificaciones.service;

import java.util.List;

import com.tamaka.notificaciones.dto.response.NotificacionResponse;

/**
 * Casos de uso de notificaciones. notificar() lo invocan los consumidores de eventos.
 */
public interface NotificacionService {

    /** Crea el aviso con la plantilla en el idioma del destinatario. */
    void notificar(Long destinatarioId, String tipoEvento, String idioma);

    List<NotificacionResponse> misNotificaciones(Long usuarioId);

    void marcarLeida(Long usuarioId, String notificacionId);
}
