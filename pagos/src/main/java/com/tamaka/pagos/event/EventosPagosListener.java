package com.tamaka.pagos.event;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.tamaka.common.event.DomainEvent;
import com.tamaka.common.event.ManejadorEvento;
import com.tamaka.pagos.service.PagoService;

/**
 * Consumidor de eventos de Reservas (arquitectura sec. 5): ReservaAceptada, ReservaCancelada
 * y PagoRequiereRevision (pago tardio). Solo traduce el evento y delega en la capa de servicio.
 */
@Component
public class EventosPagosListener implements ManejadorEvento {

    static final String RESERVA_ACEPTADA = "ReservaAceptada";
    static final String RESERVA_CANCELADA = "ReservaCancelada";
    static final String PAGO_REQUIERE_REVISION = "PagoRequiereRevision";

    private final PagoService pagoService;

    public EventosPagosListener(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @Override
    public Set<String> tiposSoportados() {
        return Set.of(RESERVA_ACEPTADA, RESERVA_CANCELADA, PAGO_REQUIERE_REVISION);
    }

    @Override
    public void manejar(DomainEvent evento) {
        Long reservaId = evento.agregadoIdLong();
        switch (evento.tipo()) {
            case RESERVA_ACEPTADA -> pagoService.registrarReservaAceptada(
                    reservaId,
                    evento.datoLongRequerido("turistaId"),
                    evento.datoLong("operadorId"),
                    evento.datoDecimalRequerido("valorAPagar"),
                    evento.datoInstanteRequerido("plazoPago"));
            case RESERVA_CANCELADA -> pagoService.registrarReservaCancelada(reservaId);
            case PAGO_REQUIERE_REVISION -> pagoService.procesarRevisionPago(reservaId);
            default -> {
                // Tipo no atendido: el despachador solo entrega los declarados.
            }
        }
    }
}
