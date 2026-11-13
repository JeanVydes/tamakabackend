package com.tamaka.common.event.outbox;

import java.time.Instant;

import org.springframework.data.annotation.Id;

/**
 * Documento MongoDB del outbox. La coleccion se indica por configuracion ({@code tamaka.eventos.outbox.coleccion}).
 */
public class OutboxDocumento {

    @Id
    private String id;

    private String tipo;

    private String agregadoId;

    private String payload;

    private String estado;

    private int intentos;

    private Instant proximoIntento;

    private Instant creadoEn;

    private Instant enviadoEn;

    private String ultimoError;

    protected OutboxDocumento() {
    }

    public OutboxDocumento(String id, String tipo, String agregadoId, String payload, Instant ahora) {
        this.id = id;
        this.tipo = tipo;
        this.agregadoId = agregadoId;
        this.payload = payload;
        this.estado = EstadoOutbox.PENDIENTE.name();
        this.intentos = 0;
        this.proximoIntento = ahora;
        this.creadoEn = ahora;
    }

    public String getId() {
        return id;
    }

    public String getTipo() {
        return tipo;
    }

    public String getAgregadoId() {
        return agregadoId;
    }

    public String getPayload() {
        return payload;
    }

    public String getEstado() {
        return estado;
    }

    public int getIntentos() {
        return intentos;
    }

    public Instant getProximoIntento() {
        return proximoIntento;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Instant getEnviadoEn() {
        return enviadoEn;
    }

    public String getUltimoError() {
        return ultimoError;
    }
}
