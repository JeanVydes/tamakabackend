package com.tamaka.reservas.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tamaka.reservas.domain.HistorialEstadoReserva;

public interface HistorialEstadoReservaRepository extends JpaRepository<HistorialEstadoReserva, Long> {
}
