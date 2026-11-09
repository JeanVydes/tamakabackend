package com.tamaka.pagos.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tamaka.pagos.domain.EstadoPago;
import com.tamaka.pagos.domain.Pago;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    Optional<Pago> findByReferenciaPasarela(String referenciaPasarela);

    Optional<Pago> findFirstByReservaIdAndEstado(Long reservaId, EstadoPago estado);

    List<Pago> findByTuristaIdOrderByCreadoEnDesc(Long turistaId);
}
