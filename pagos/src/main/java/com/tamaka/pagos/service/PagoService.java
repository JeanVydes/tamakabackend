package com.tamaka.pagos.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.tamaka.pagos.dto.request.IniciarPagoRequest;
import com.tamaka.pagos.dto.request.WebhookPasarelaRequest;
import com.tamaka.pagos.dto.response.ComprobanteResponse;
import com.tamaka.pagos.dto.response.PagoResponse;

/**
 * Casos de uso de Pagos y entradas desde eventos de Reservas (proyeccion local).
 */
public interface PagoService {

    PagoResponse iniciar(Long turistaId, IniciarPagoRequest request);

    List<PagoResponse> misPagos(Long turistaId);

    ComprobanteResponse comprobante(Long turistaId, Long pagoId);

    /** Webhook de la pasarela. Verifica la firma antes de procesar; es idempotente. */
    void procesarWebhook(String firma, WebhookPasarelaRequest request);

    /** Evento ReservaAceptada. */
    void registrarReservaAceptada(Long reservaId, Long turistaId, BigDecimal valorAPagar, Instant plazoPago);

    /** Evento ReservaCancelada. */
    void registrarReservaCancelada(Long reservaId);

    /** Evento PagoRequiereRevision: pago tardio sobre una reserva que ya no estaba aceptada. Se reembolsa. */
    void procesarRevisionPago(Long reservaId);
}
