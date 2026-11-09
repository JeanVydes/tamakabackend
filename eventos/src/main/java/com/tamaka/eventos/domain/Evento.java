package com.tamaka.eventos.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

import com.tamaka.eventos.exception.RangoFechasInvalidoException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Evento turistico publicado por un gestor. Se desactiva; no se borra (historial).
 */
@Entity
@Table(name = "evento")
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(length = 2000)
    private String descripcion;

    @Column(nullable = false, length = 200)
    private String lugar;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(name = "gestor_id", nullable = false)
    private Long gestorId;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    protected Evento() {
    }

    private Evento(String titulo, String descripcion, String lugar,
                   LocalDate fechaInicio, LocalDate fechaFin, Long gestorId) {
        if (fechaFin.isBefore(fechaInicio)) {
            throw new RangoFechasInvalidoException(fechaInicio, fechaFin);
        }
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.lugar = lugar;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.gestorId = gestorId;
        this.activo = true;
        this.creadoEn = Instant.now();
    }

    public static Evento publicar(String titulo, String descripcion, String lugar,
                                  LocalDate fechaInicio, LocalDate fechaFin, Long gestorId) {
        return new Evento(titulo, descripcion, lugar, fechaInicio, fechaFin, gestorId);
    }

    public void actualizar(String titulo, String descripcion, String lugar,
                           LocalDate fechaInicio, LocalDate fechaFin) {
        if (fechaFin.isBefore(fechaInicio)) {
            throw new RangoFechasInvalidoException(fechaInicio, fechaFin);
        }
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.lugar = lugar;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
    }

    public void desactivar() {
        this.activo = false;
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getLugar() {
        return lugar;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public Long getGestorId() {
        return gestorId;
    }

    public boolean isActivo() {
        return activo;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        return otro instanceof Evento that && id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
