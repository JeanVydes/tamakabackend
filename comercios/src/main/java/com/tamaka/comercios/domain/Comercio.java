package com.tamaka.comercios.domain;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Agregado Comercio. Pertenece a un unico operador (operadorId, identificador del token).
 */
@Entity
@Table(name = "comercio")
public class Comercio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operador_id", nullable = false)
    private Long operadorId;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(nullable = false, unique = true, length = 50)
    private String rnt;

    @Column(nullable = false, length = 60)
    private String categoria;

    @Column(length = 1000)
    private String descripcion;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    protected Comercio() {
    }

    private Comercio(Long operadorId, String nombre, String rnt, String categoria, String descripcion) {
        this.operadorId = operadorId;
        this.nombre = nombre;
        this.rnt = rnt;
        this.categoria = categoria;
        this.descripcion = descripcion;
        this.activo = true;
        this.creadoEn = Instant.now();
    }

    public static Comercio crear(Long operadorId, String nombre, String rnt, String categoria, String descripcion) {
        return new Comercio(operadorId, nombre, rnt, categoria, descripcion);
    }

    public void actualizar(String nombre, String categoria, String descripcion) {
        this.nombre = nombre;
        this.categoria = categoria;
        this.descripcion = descripcion;
    }

    public boolean esDe(Long operadorId) {
        return this.operadorId.equals(operadorId);
    }

    public Long getId() {
        return id;
    }

    public Long getOperadorId() {
        return operadorId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getRnt() {
        return rnt;
    }

    public String getCategoria() {
        return categoria;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean isActivo() {
        return activo;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        return otro instanceof Comercio that && id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
