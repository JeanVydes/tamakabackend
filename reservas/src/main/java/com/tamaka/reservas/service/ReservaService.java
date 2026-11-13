package com.tamaka.reservas.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import com.tamaka.reservas.domain.EstadoReserva;
import com.tamaka.reservas.dto.request.CancelarReservaRequest;
import com.tamaka.reservas.dto.request.CrearReservaRequest;
import com.tamaka.reservas.dto.request.ResponderSolicitudRequest;
import com.tamaka.reservas.dto.response.ReservaResponse;

/**
 * Casos de uso de Reservas. Los identificadores de usuario provienen del token ya validado.
 */
public interface ReservaService {

    ReservaResponse solicitar(Long turistaId, CrearReservaRequest request);

    List<ReservaResponse> misReservas(Long turistaId);

    /** Detalle para el turista dueno de la reserva (403 si es ajena). */
    ReservaResponse detalleComoTurista(Long turistaId, Long reservaId);

    ReservaResponse cancelarComoTurista(Long turistaId, Long reservaId);

    List<ReservaResponse> reservasDelOperador(Long operadorId);

    /** Detalle para el operador dueno del servicio (403 si es ajena). */
    ReservaResponse detalleComoOperador(Long operadorId, Long reservaId);

    ReservaResponse responder(Long operadorId, Long reservaId, ResponderSolicitudRequest request);

    /** El operador cancela una solicitud que ya acepto (ACEPTADA o CONFIRMADA). Pagos reembolsa si estaba pagada. */
    ReservaResponse cancelarComoOperador(Long operadorId, Long reservaId, CancelarReservaRequest request);

    /** Listado de administracion, opcionalmente filtrado por estado. */
    List<ReservaResponse> listarTodas(EstadoReserva estado);

    /**
     * Entrada desde Pagos (PagoAprobado). Idempotente: devuelve true si la reserva quedo (o ya estaba) CONFIRMADA;
     * false si ya no estaba ACEPTADA (p. ej., cancelada por timeout) y debe revisarse el reembolso.
     */
    boolean confirmarPorPago(Long reservaId);

    /**
     * Entrada desde Comercios (ServicioDesactivado): cancela las solicitudes PENDIENTE y ACEPTADA del servicio.
     * Devuelve el numero de solicitudes canceladas.
     */
    int cancelarPorServicioDesactivado(Long servicioId);

    /**
     * Cancela solicitudes vencidas: PENDIENTE sin respuesta (RF-RES-15) y ACEPTADA sin pago (RF-RES-14).
     * Devuelve el numero de solicitudes canceladas.
     */
    int cancelarVencidas(Instant ahora);

    /** Pasa a FINALIZADA las reservas CONFIRMADA cuya fecha ya paso. Devuelve cuantas finalizo. */
    int finalizarConcluidas(LocalDate hoy);
}
