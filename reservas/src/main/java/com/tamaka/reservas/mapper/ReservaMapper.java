package com.tamaka.reservas.mapper;

import org.springframework.stereotype.Component;

import com.tamaka.reservas.domain.SolicitudReserva;
import com.tamaka.reservas.dto.response.ReservaResponse;

/**
 * Transforma Entity -> DTO. Sin reglas de negocio.
 */
@Component
public class ReservaMapper {

    public ReservaResponse toResponse(SolicitudReserva reserva) {
        return new ReservaResponse(
                reserva.getId(),
                reserva.getTuristaId(),
                reserva.getServicioId(),
                reserva.getOperadorId(),
                reserva.getFechaReserva(),
                reserva.getNumeroPersonas(),
                reserva.getEstado().name(),
                reserva.getValorAPagar(),
                reserva.getObservacionOperador(),
                reserva.getCreadaEn(),
                reserva.getPlazoPago());
    }
}
