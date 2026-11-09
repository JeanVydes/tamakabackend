package com.tamaka.cuentas.domain;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import com.tamaka.cuentas.exception.CuentaYaSuspendidaException;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

/**
 * Agregado de autenticacion. Un unico registro por persona, con sus roles.
 * Las transiciones (suspender, reactivar, agregar rol) viven en la entidad.
 */
@Entity
@Table(name = "cuenta")
public class Cuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 180)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false)
    private boolean activa;

    @Column(name = "modo_operador", nullable = false)
    private boolean modoOperador;

    @Column(name = "creada_en", nullable = false)
    private Instant creadaEn;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "cuenta_rol", joinColumns = @JoinColumn(name = "cuenta_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 20)
    private Set<Rol> roles = EnumSet.noneOf(Rol.class);

    protected Cuenta() {
    }

    private Cuenta(String email, String passwordHash, String nombre) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.nombre = nombre;
        this.activa = true;
        this.modoOperador = false;
        this.creadaEn = Instant.now();
        this.roles.add(Rol.TURISTA);
    }

    public static Cuenta registrar(String email, String passwordHash, String nombre) {
        return new Cuenta(email, passwordHash, nombre);
    }

    public void suspender() {
        if (!activa) {
            throw new CuentaYaSuspendidaException(id);
        }
        this.activa = false;
    }

    public void reactivar() {
        this.activa = true;
    }

    public void agregarRol(Rol rol) {
        this.roles.add(rol);
        if (rol == Rol.OPERADOR) {
            this.modoOperador = true;
        }
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean isActiva() {
        return activa;
    }

    public boolean isModoOperador() {
        return modoOperador;
    }

    public Instant getCreadaEn() {
        return creadaEn;
    }

    public Set<Rol> getRoles() {
        return Set.copyOf(roles);
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        if (!(otro instanceof Cuenta that)) {
            return false;
        }
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
