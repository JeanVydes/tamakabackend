package com.tamaka.atractivos.domain;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Atractivo turistico. Su disponibilidad vive aqui (RF-ATT-04, RF-ATT-09), no en un modulo de cupos.
 */
@Entity
@Table(name = "atractivo")
public class Atractivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(length = 2000)
    private String descripcion;

    @Column(nullable = false, length = 60)
    private String categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoAtractivo estado;

    @Column(name = "gestor_id", nullable = false)
    private Long gestorId;

    /** Clave del objeto en R2; null si el atractivo no tiene imagen de portada. */
    @Column(name = "imagen_key", length = 512)
    private String imagenKey;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    protected Atractivo() {
    }

    private Atractivo(String nombre, String descripcion, String categoria, Long gestorId) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.categoria = categoria;
        this.gestorId = gestorId;
        this.estado = EstadoAtractivo.ABIERTO;
        this.creadoEn = Instant.now();
    }

    public static Atractivo crear(String nombre, String descripcion, String categoria, Long gestorId) {
        return new Atractivo(nombre, descripcion, categoria, gestorId);
    }

    public void cambiarEstado(EstadoAtractivo nuevo) {
        this.estado = nuevo;
    }

    /** Guarda la clave de la nueva portada (o null al eliminarla). */
    public void cambiarImagen(String nuevaClave) {
        this.imagenKey = nuevaClave;
    }

    public String getImagenKey() {
        return imagenKey;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getCategoria() {
        return categoria;
    }

    public EstadoAtractivo getEstado() {
        return estado;
    }

    public Long getGestorId() {
        return gestorId;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        return otro instanceof Atractivo that && id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
