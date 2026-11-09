package com.tamaka.preferencias.domain;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Preferencias de un turista (documento MongoDB: esquema variable por turista).
 */
@Document(collection = "preferencias")
public class Preferencia {

    @Id
    private String id;

    @Indexed(unique = true)
    private Long turistaId;

    private List<String> categorias = new ArrayList<>();

    private String idioma;

    protected Preferencia() {
    }

    public Preferencia(Long turistaId) {
        this.turistaId = turistaId;
        this.idioma = "es";
    }

    public void actualizar(List<String> categorias, String idioma) {
        this.categorias = new ArrayList<>(categorias);
        this.idioma = idioma;
    }

    public String getId() {
        return id;
    }

    public Long getTuristaId() {
        return turistaId;
    }

    public List<String> getCategorias() {
        return List.copyOf(categorias);
    }

    public String getIdioma() {
        return idioma;
    }
}
