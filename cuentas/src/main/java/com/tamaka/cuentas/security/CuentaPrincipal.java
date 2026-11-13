package com.tamaka.cuentas.security;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.tamaka.cuentas.domain.Cuenta;
import com.tamaka.cuentas.domain.Rol;

/**
 * Identidad autenticada de Cuentas. Solo Cuentas posee tabla de usuarios; los demas servicios
 * reconstruyen la identidad a partir de los claims del token.
 */
public class CuentaPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String passwordHash;
    private final boolean activa;
    private final Set<Rol> roles;

    public CuentaPrincipal(Cuenta cuenta) {
        this.id = cuenta.getId();
        this.email = cuenta.getEmail();
        this.passwordHash = cuenta.getPasswordHash();
        this.activa = cuenta.isActiva();
        // Solo los roles del modo activo (sin OPERADOR mientras la cuenta esta en modo turista).
        this.roles = cuenta.rolesActivos();
    }

    public Long getId() {
        return id;
    }

    public List<String> getRolesNombres() {
        return roles.stream().map(Enum::name).sorted().toList();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(rol -> new SimpleGrantedAuthority("ROLE_" + rol.name()))
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return activa;
    }
}
