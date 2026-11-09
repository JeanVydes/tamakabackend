package com.tamaka.reservas.service;

import java.time.Instant;
import java.util.List;

import com.tamaka.reservas.dto.request.CrearReservaRequest;
import com.tamaka.reservas.dto.request.ResponderSolicitudRequest;
import com.tamaka.reservas.dto.response.ReservaResponse;

/**
 * Casos de uso de Reservas. Los identificadores de usuario provienen del token ya validado.
 */
public interface ReservaService {

    ReservaResponse solicitar(Long turistaId, CrearReservaRequest request);

    List<ReservaResponse> misReservas(Long turistaId);

    ReservaResponse cancelarComoTurista(Long turistaId, Long reservaId);

    List<ReservaResponse> reservasDelOperador(Long operadorId);

    ReservaResponse responder(Long operadorId, Long reservaId, ResponderSolicitudRequest request);

    /**
     * Entrada desde Pagos (PagoAprobado). Devuelve true si la reserva quedo CONFIRMADA;
     * false si ya no estaba ACEPTADA (p. ej., cancelada por timeout) y debe revisarse el reembolso.
     */
    boolean confirmarPorPago(Long reservaId);

    /**
     * Cancela solicitudes vencidas: PENDIENTE sin respuesta (RF-RES-15) y ACEPTADA sin pago (RF-RES-14).
     * Devuelve el numero de solicitudes canceladas.
     */
    int cancelarVencidas(Instant ahora);
}
