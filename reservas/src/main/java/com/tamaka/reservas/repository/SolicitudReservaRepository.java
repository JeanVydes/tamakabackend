package com.tamaka.reservas.repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tamaka.reservas.domain.EstadoReserva;
import com.tamaka.reservas.domain.SolicitudReserva;

public interface SolicitudReservaRepository extends JpaRepository<SolicitudReserva, Long> {

    List<SolicitudReserva> findByTuristaIdOrderByCreadaEnDesc(Long turistaId);

    List<SolicitudReserva> findByOperadorIdOrderByCreadaEnDesc(Long operadorId);

    List<SolicitudReserva> findByEstadoAndCreadaEnBefore(EstadoReserva estado, Instant limite);

    List<SolicitudReserva> findByEstadoAndPlazoPagoBefore(EstadoReserva estado, Instant limite);

    /** Solicitudes de un servicio en ciertos estados (indice servicio_id, estado). */
    List<SolicitudReserva> findByServicioIdAndEstadoIn(Long servicioId, Collection<EstadoReserva> estados);

    /** Confirmadas cuya fecha ya paso (job de finalizacion). */
    List<SolicitudReserva> findByEstadoAndFechaReservaBefore(EstadoReserva estado, LocalDate fecha);

    /** Listado de administracion. */
    List<SolicitudReserva> findAllByOrderByCreadaEnDesc();

    List<SolicitudReserva> findByEstadoOrderByCreadaEnDesc(EstadoReserva estado);
}
