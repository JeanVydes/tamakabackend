package com.tamaka.notificaciones.domain;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Marca de idempotencia: id de un evento ya convertido en avisos. Crear una notificacion no es idempotente por si
 * mismo, asi que una reentrega del bus (al menos una vez) se descarta si su id ya esta aqui.
 * Se conservan 30 dias (indice TTL), mas que la ventana de reintentos del outbox.
 */
@Document(collection = "eventos_procesados")
public class EventoProcesado {

    @Id
    private String eventoId;

    private String tipo;

    @Indexed(expireAfter = "30d")
    private Instant procesadoEn;

    protected EventoProcesado() {
    }

    public EventoProcesado(String eventoId, String tipo) {
        this.eventoId = eventoId;
        this.tipo = tipo;
        this.procesadoEn = Instant.now();
    }

    public String getEventoId() {
        return eventoId;
    }

    public String getTipo() {
        return tipo;
    }

    public Instant getProcesadoEn() {
        return procesadoEn;
    }
}
