package com.tamaka.reservas.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Registro append-only de cada transicion de estado (RNF Auditoria). Nunca se actualiza.
 */
@Entity
@Table(name = "historial_estado_reserva")
public class HistorialEstadoReserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reserva_id", nullable = false)
    private Long reservaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 20)
    private EstadoReserva estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 20)
    private EstadoReserva estadoNuevo;

    @Column(length = 300)
    private String motivo;

    @Column(name = "ocurrido_en", nullable = false)
    private Instant ocurridoEn;

    protected HistorialEstadoReserva() {
    }

    public HistorialEstadoReserva(Long reservaId, EstadoReserva anterior, EstadoReserva nuevo, String motivo) {
        this.reservaId = reservaId;
        this.estadoAnterior = anterior;
        this.estadoNuevo = nuevo;
        this.motivo = motivo;
        this.ocurridoEn = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getReservaId() {
        return reservaId;
    }

    public EstadoReserva getEstadoNuevo() {
        return estadoNuevo;
    }
}
