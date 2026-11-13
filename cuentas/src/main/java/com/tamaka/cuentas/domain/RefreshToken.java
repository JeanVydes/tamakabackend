package com.tamaka.cuentas.domain;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Refresh token opaco de una cuenta. Solo se persiste el hash; el valor en claro se entrega una vez al cliente.
 */
@Entity
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cuenta_id", nullable = false)
    private Long cuentaId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "emitido_en", nullable = false)
    private Instant emitidoEn;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    @Column(name = "revocado_en")
    private Instant revocadoEn;

    protected RefreshToken() {
    }

    public RefreshToken(Long cuentaId, String tokenHash, Instant emitidoEn, Instant expiraEn) {
        this.cuentaId = cuentaId;
        this.tokenHash = tokenHash;
        this.emitidoEn = emitidoEn;
        this.expiraEn = expiraEn;
    }

    /** Vigente: no revocado y sin expirar. */
    public boolean vigenteEn(Instant ahora) {
        return revocadoEn == null && expiraEn.isAfter(ahora);
    }

    public boolean isRevocado() {
        return revocadoEn != null;
    }

    /** Idempotente: revocar dos veces conserva la primera fecha. */
    public void revocar(Instant ahora) {
        if (revocadoEn == null) {
            this.revocadoEn = ahora;
        }
    }

    public Long getId() {
        return id;
    }

    public Long getCuentaId() {
        return cuentaId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getEmitidoEn() {
        return emitidoEn;
    }

    public Instant getExpiraEn() {
        return expiraEn;
    }

    public Instant getRevocadoEn() {
        return revocadoEn;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        return otro instanceof RefreshToken that && id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
