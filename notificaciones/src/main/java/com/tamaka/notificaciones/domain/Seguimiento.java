package com.tamaka.notificaciones.domain;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Proyeccion local de los favoritos del turista (eventos FavoritoGuardado / FavoritoEliminado).
 * Permite avisar a quienes siguen un atractivo cuando cambia o tiene una alerta de seguridad.
 */
@Document(collection = "seguimientos")
@CompoundIndex(name = "uk_seguimiento_turista_tipo_ref", def = "{'turistaId': 1, 'tipo': 1, 'refId': 1}", unique = true)
public class Seguimiento {

    @Id
    private String id;

    private Long turistaId;

    private String tipo;

    private String refId;

    private Instant creadoEn;

    protected Seguimiento() {
    }

    public Seguimiento(Long turistaId, String tipo, String refId) {
        this.turistaId = turistaId;
        this.tipo = tipo;
        this.refId = refId;
        this.creadoEn = Instant.now();
    }

    public String getId() {
        return id;
    }

    public Long getTuristaId() {
        return turistaId;
    }

    public String getTipo() {
        return tipo;
    }

    public String getRefId() {
        return refId;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}
