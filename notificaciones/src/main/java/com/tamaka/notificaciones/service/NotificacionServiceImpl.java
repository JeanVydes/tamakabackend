package com.tamaka.notificaciones.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.tamaka.common.error.ResourceNotFoundException;
import com.tamaka.notificaciones.domain.Notificacion;
import com.tamaka.notificaciones.dto.response.NotificacionResponse;
import com.tamaka.notificaciones.exception.TipoNotificacionDesconocidoException;
import com.tamaka.notificaciones.repository.NotificacionRepository;

/**
 * Pendiente: canal de correo y push (SES / FCM). Hoy la notificacion queda persistida y registrada en el log.
 */
@Service
public class NotificacionServiceImpl implements NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionServiceImpl.class);

    private final NotificacionRepository notificacionRepository;

    public NotificacionServiceImpl(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    @Override
    public void notificar(Long destinatarioId, String tipoEvento, String idioma) {
        PlantillasNotificacion.Texto texto = PlantillasNotificacion.buscar(tipoEvento, idioma)
                .orElseThrow(() -> new TipoNotificacionDesconocidoException(tipoEvento));
        Notificacion notificacion = new Notificacion(destinatarioId, tipoEvento,
                "en".equals(idioma) ? "en" : "es", texto.titulo(), texto.cuerpo());
        notificacionRepository.save(notificacion);
        log.info("notificacion creada tipo={}", tipoEvento);
    }

    @Override
    public List<NotificacionResponse> misNotificaciones(Long usuarioId) {
        return notificacionRepository.findByDestinatarioIdOrderByCreadaEnDesc(usuarioId).stream()
                .map(n -> new NotificacionResponse(n.getId(), n.getTipo(), n.getTitulo(), n.getCuerpo(),
                        n.isLeida(), n.getCreadaEn()))
                .toList();
    }

    @Override
    public void marcarLeida(Long usuarioId, String notificacionId) {
        // Mismo error si no existe o es ajena: no se revela si el id pertenece a otro usuario.
        Notificacion notificacion = notificacionRepository.findByIdAndDestinatarioId(notificacionId, usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificacion", notificacionId));
        notificacion.marcarLeida();
        notificacionRepository.save(notificacion);
    }
}
