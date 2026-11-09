package com.tamaka.reservas.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tamaka.reservas.domain.EstadoReserva;
import com.tamaka.reservas.domain.SolicitudReserva;

public interface SolicitudReservaRepository extends JpaRepository<SolicitudReserva, Long> {

    List<SolicitudReserva> findByTuristaIdOrderByCreadaEnDesc(Long turistaId);

    List<SolicitudReserva> findByOperadorIdOrderByCreadaEnDesc(Long operadorId);

    List<SolicitudReserva> findByEstadoAndCreadaEnBefore(EstadoReserva estado, Instant limite);

    List<SolicitudReserva> findByEstadoAndPlazoPagoBefore(EstadoReserva estado, Instant limite);
}
