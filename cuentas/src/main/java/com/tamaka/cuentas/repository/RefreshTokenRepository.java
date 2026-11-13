package com.tamaka.cuentas.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tamaka.cuentas.domain.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Revoca todos los refresh tokens activos de la cuenta. Devuelve cuantos revoco. */
    @Modifying
    @Query("update RefreshToken r set r.revocadoEn = :ahora where r.cuentaId = :cuentaId and r.revocadoEn is null")
    int revocarActivos(@Param("cuentaId") Long cuentaId, @Param("ahora") Instant ahora);
}
