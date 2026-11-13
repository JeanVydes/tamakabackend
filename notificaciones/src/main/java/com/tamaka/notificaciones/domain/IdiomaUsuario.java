package com.tamaka.notificaciones.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Proyeccion local del idioma preferido de cada usuario, construida con el evento PreferenciasActualizadas.
 * Notificaciones no lee la base de Preferencias.
 */
@Document(collection = "idiomas_usuario")
public class IdiomaUsuario {

    public static final String IDIOMA_POR_DEFECTO = "es";

    @Id
    private Long usuarioId;

    private String idioma;

    protected IdiomaUsuario() {
    }

    public IdiomaUsuario(Long usuarioId, String idioma) {
        this.usuarioId = usuarioId;
        cambiar(idioma);
    }

    /** Solo se admiten es y en; cualquier otro valor cae al idioma por defecto. */
    public void cambiar(String nuevo) {
        this.idioma = "en".equals(nuevo) ? "en" : IDIOMA_POR_DEFECTO;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public String getIdioma() {
        return idioma;
    }
}
