package com.tamaka.notificaciones.domain;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Aviso dirigido a un usuario. Documento con plantilla ya resuelta en el idioma del destinatario.
 */
@Document(collection = "notificaciones")
public class Notificacion {

    @Id
    private String id;

    @Indexed
    private Long destinatarioId;

    private String tipo;

    private String idioma;

    private String titulo;

    private String cuerpo;

    private boolean leida;

    private Instant creadaEn;

    protected Notificacion() {
    }

    public Notificacion(Long destinatarioId, String tipo, String idioma, String titulo, String cuerpo) {
        this.destinatarioId = destinatarioId;
        this.tipo = tipo;
        this.idioma = idioma;
        this.titulo = titulo;
        this.cuerpo = cuerpo;
        this.leida = false;
        this.creadaEn = Instant.now();
    }

    public void marcarLeida() {
        this.leida = true;
    }

    public String getId() {
        return id;
    }

    public Long getDestinatarioId() {
        return destinatarioId;
    }

    public String getTipo() {
        return tipo;
    }

    public String getIdioma() {
        return idioma;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getCuerpo() {
        return cuerpo;
    }

    public boolean isLeida() {
        return leida;
    }

    public Instant getCreadaEn() {
        return creadaEn;
    }
}
