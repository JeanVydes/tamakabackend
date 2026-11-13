package com.tamaka.common.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adaptador de respaldo: registra el evento en el log sin datos personales.
 * Se usa con {@code tamaka.eventos.transporte=log} y cuando un tipo de evento no tiene suscriptores configurados.
 */
public class LoggingEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LoggingEventPublisher.class);

    @Override
    public void publicar(DomainEvent evento) {
        log.info("evento publicado tipo={} agregado={}", evento.tipo(), evento.agregadoId());
    }
}
