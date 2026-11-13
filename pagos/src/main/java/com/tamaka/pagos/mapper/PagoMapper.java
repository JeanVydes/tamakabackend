package com.tamaka.pagos.mapper;

import org.springframework.stereotype.Component;

import com.tamaka.pagos.domain.Pago;
import com.tamaka.pagos.dto.response.ComprobanteResponse;
import com.tamaka.pagos.dto.response.PagoDetalleResponse;
import com.tamaka.pagos.dto.response.PagoResponse;

/**
 * Transforma Entity -> DTO. Sin reglas de negocio.
 */
@Component
public class PagoMapper {

    public PagoResponse toResponse(Pago pago, String urlPago) {
        return new PagoResponse(
                pago.getId(),
                pago.getReservaId(),
                pago.getMonto(),
                pago.getEstado().name(),
                pago.getReferenciaPasarela(),
                urlPago);
    }

    public ComprobanteResponse toComprobante(Pago pago) {
        return new ComprobanteResponse(
                pago.getId(),
                pago.getReservaId(),
                pago.getMonto(),
                pago.getEstado().name(),
                pago.getReferenciaPasarela(),
                pago.getAprobadoEn());
    }

    public PagoDetalleResponse toDetalle(Pago pago) {
        return new PagoDetalleResponse(
                pago.getId(),
                pago.getReservaId(),
                pago.getTuristaId(),
                pago.getOperadorId(),
                pago.getMonto(),
                pago.getEstado().name(),
                pago.getReferenciaPasarela(),
                pago.getCreadoEn(),
                pago.getAprobadoEn());
    }
}
