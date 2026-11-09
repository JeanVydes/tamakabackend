package com.tamaka.atractivos.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Alerta de seguridad emitida por un gestor sobre un atractivo. Registro append-only.
 */
@Entity
@Table(name = "alerta_seguridad")
public class AlertaSeguridad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "atractivo_id", nullable = false)
    private Long atractivoId;

    @Column(nullable = false, length = 500)
    private String mensaje;

    @Column(name = "emitida_por", nullable = false)
    private Long emitidaPor;

    @Column(name = "emitida_en", nullable = false)
    private Instant emitidaEn;

    protected AlertaSeguridad() {
    }

    public AlertaSeguridad(Long atractivoId, String mensaje, Long emitidaPor) {
        this.atractivoId = atractivoId;
        this.mensaje = mensaje;
        this.emitidaPor = emitidaPor;
        this.emitidaEn = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getAtractivoId() {
        return atractivoId;
    }

    public String getMensaje() {
        return mensaje;
    }

    public Instant getEmitidaEn() {
        return emitidaEn;
    }
}
