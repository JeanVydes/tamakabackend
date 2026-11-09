package com.tamaka.preferencias.domain;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Favorito del turista sobre un atractivo, servicio o evento. Un mismo elemento no se guarda dos veces.
 */
@Document(collection = "favoritos")
@CompoundIndex(name = "uk_favorito_turista_tipo_ref", def = "{'turistaId': 1, 'tipo': 1, 'refId': 1}", unique = true)
public class Favorito {

    @Id
    private String id;

    private Long turistaId;

    private String tipo;

    private String refId;

    private Instant creadoEn;

    protected Favorito() {
    }

    public Favorito(Long turistaId, String tipo, String refId) {
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
}
