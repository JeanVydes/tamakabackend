package com.tamaka.indicadores.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Hecho de demanda registrado desde un evento. Append-only.
 */
@Entity
@Table(name = "registro_demanda")
public class RegistroDemanda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String tipo;

    @Column(name = "operador_id")
    private Long operadorId;

    @Column(name = "referencia_id", nullable = false)
    private Long referenciaId;

    @Column(nullable = false)
    private LocalDate dia;

    @Column(name = "ocurrido_en", nullable = false)
    private Instant ocurridoEn;

    protected RegistroDemanda() {
    }

    public RegistroDemanda(String tipo, Long operadorId, Long referenciaId, Instant ocurridoEn) {
        this.tipo = tipo;
        this.operadorId = operadorId;
        this.referenciaId = referenciaId;
        this.ocurridoEn = ocurridoEn;
        this.dia = ocurridoEn.atZone(ZoneOffset.UTC).toLocalDate();
    }

    public String getTipo() {
        return tipo;
    }

    public Long getOperadorId() {
        return operadorId;
    }
}
