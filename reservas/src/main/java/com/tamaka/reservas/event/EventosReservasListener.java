package com.tamaka.reservas.event;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.tamaka.common.event.DomainEvent;
import com.tamaka.common.event.ManejadorEvento;
import com.tamaka.reservas.service.ReservaService;

/**
 * Consumidor de eventos de Reservas (arquitectura sec. 5): PagoAprobado y ServicioDesactivado.
 * Solo traduce el evento y delega en la capa de servicio.
 */
@Component
public class EventosReservasListener implements ManejadorEvento {

    static final String PAGO_APROBADO = "PagoAprobado";
    static final String SERVICIO_DESACTIVADO = "ServicioDesactivado";

    private final ReservaService reservaService;

    public EventosReservasListener(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @Override
    public Set<String> tiposSoportados() {
        return Set.of(PAGO_APROBADO, SERVICIO_DESACTIVADO);
    }

    @Override
    public void manejar(DomainEvent evento) {
        switch (evento.tipo()) {
            case PAGO_APROBADO -> reservaService.confirmarPorPago(evento.datoLongRequerido("reservaId"));
            case SERVICIO_DESACTIVADO -> reservaService.cancelarPorServicioDesactivado(evento.agregadoIdLong());
            default -> {
                // Tipo no atendido: el despachador solo entrega los declarados.
            }
        }
    }
}
