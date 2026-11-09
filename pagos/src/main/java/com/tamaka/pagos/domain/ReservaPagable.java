package com.tamaka.pagos.domain;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Proyeccion local de una reserva aceptada, construida con eventos. Permite validar el cobro sin leer Reservas.
 */
@Entity
@Table(name = "reserva_pagable")
public class ReservaPagable {

    @Id
    @Column(name = "reserva_id")
    private Long reservaId;

    @Column(name = "turista_id", nullable = false)
    private Long turistaId;

    @Column(name = "valor_a_pagar", nullable = false, precision = 12, scale = 2)
    private BigDecimal valorAPagar;

    @Column(name = "plazo_pago", nullable = false)
    private Instant plazoPago;

    @Column(nullable = false)
    private boolean cancelada;

    protected ReservaPagable() {
    }

    public ReservaPagable(Long reservaId, Long turistaId, BigDecimal valorAPagar, Instant plazoPago) {
        this.reservaId = reservaId;
        this.turistaId = turistaId;
        this.valorAPagar = valorAPagar;
        this.plazoPago = plazoPago;
        this.cancelada = false;
    }

    public void cancelar() {
        this.cancelada = true;
    }

    public boolean pagableEn(Instant ahora) {
        return !cancelada && plazoPago.isAfter(ahora);
    }

    public Long getReservaId() {
        return reservaId;
    }

    public Long getTuristaId() {
        return turistaId;
    }

    public BigDecimal getValorAPagar() {
        return valorAPagar;
    }

    public Instant getPlazoPago() {
        return plazoPago;
    }
}
