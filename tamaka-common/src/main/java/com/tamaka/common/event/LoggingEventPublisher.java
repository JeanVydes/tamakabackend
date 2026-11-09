package com.tamaka.common.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Adaptador por defecto: registra el evento en el log sin datos personales.
 * Pendiente: adaptador SNS (publicacion) y listeners SQS (consumo) en la fase de integracion.
 */
@Component
public class LoggingEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingEventPublisher.class);

    @Override
    public void publicar(DomainEvent evento) {
        log.info("evento publicado tipo={} agregado={}", evento.tipo(), evento.agregadoId());
    }
}
