package com.tamaka.cuentas.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tamaka.cuentas.domain.Cuenta;

public interface CuentaRepository extends JpaRepository<Cuenta, Long> {

    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = "roles")
    Optional<Cuenta> findByEmail(String email);

    @Query("select c from Cuenta c left join fetch c.roles where c.id = :id")
    Optional<Cuenta> findConRolesPorId(@Param("id") Long id);
}
