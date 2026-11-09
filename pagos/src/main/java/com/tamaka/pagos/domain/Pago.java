package com.tamaka.pagos.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

import com.tamaka.pagos.exception.TransicionPagoInvalidaException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Entidad propia de pago asociada a una reserva (SRS 3.5). Referencia a la reserva por id, sin FK entre servicios.
 */
@Entity
@Table(name = "pago")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(name = "reserva_id", nullable = false)
    private Long reservaId;

    @Column(name = "turista_id", nullable = false)
    private Long turistaId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPago estado;

    @Column(name = "referencia_pasarela", nullable = false, unique = true, length = 100)
    private String referenciaPasarela;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "aprobado_en")
    private Instant aprobadoEn;

    protected Pago() {
    }

    private Pago(Long reservaId, Long turistaId, BigDecimal monto, String referenciaPasarela, Instant ahora) {
        this.reservaId = reservaId;
        this.turistaId = turistaId;
        this.monto = monto;
        this.referenciaPasarela = referenciaPasarela;
        this.estado = EstadoPago.PENDIENTE;
        this.creadoEn = ahora;
    }

    public static Pago iniciar(Long reservaId, Long turistaId, BigDecimal monto,
                               String referenciaPasarela, Instant ahora) {
        return new Pago(reservaId, turistaId, monto, referenciaPasarela, ahora);
    }

    /**
     * PENDIENTE -> APROBADO. Devuelve false si ya estaba aprobado (webhook repetido): la operacion es idempotente.
     */
    public boolean aprobar(Instant ahora) {
        if (estado == EstadoPago.APROBADO) {
            return false;
        }
        if (estado != EstadoPago.PENDIENTE) {
            throw new TransicionPagoInvalidaException(id, estado, EstadoPago.APROBADO);
        }
        this.estado = EstadoPago.APROBADO;
        this.aprobadoEn = ahora;
        return true;
    }

    public void rechazar() {
        if (estado != EstadoPago.PENDIENTE) {
            throw new TransicionPagoInvalidaException(id, estado, EstadoPago.RECHAZADO);
        }
        this.estado = EstadoPago.RECHAZADO;
    }

    /** APROBADO -> REEMBOLSADO (pago tardio sobre una reserva que ya no estaba ACEPTADA). */
    public void reembolsar() {
        if (estado != EstadoPago.APROBADO) {
            throw new TransicionPagoInvalidaException(id, estado, EstadoPago.REEMBOLSADO);
        }
        this.estado = EstadoPago.REEMBOLSADO;
    }

    public boolean esDeTurista(Long turistaId) {
        return this.turistaId.equals(turistaId);
    }

    public Long getId() {
        return id;
    }

    public Long getReservaId() {
        return reservaId;
    }

    public Long getTuristaId() {
        return turistaId;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public EstadoPago getEstado() {
        return estado;
    }

    public String getReferenciaPasarela() {
        return referenciaPasarela;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Instant getAprobadoEn() {
        return aprobadoEn;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        return otro instanceof Pago that && id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
