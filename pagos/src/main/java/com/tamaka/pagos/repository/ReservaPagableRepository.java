package com.tamaka.pagos.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tamaka.pagos.domain.ReservaPagable;

public interface ReservaPagableRepository extends JpaRepository<ReservaPagable, Long> {
}
