package com.tamaka.comercios.event;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.tamaka.common.event.DomainEvent;
import com.tamaka.common.event.ManejadorEvento;
import com.tamaka.comercios.service.ComercioService;

/**
 * Consumidor de eventos de Comercios (arquitectura sec. 5): CuentaSuspendida.
 * Solo traduce el evento y delega en la capa de servicio.
 */
@Component
public class EventosComerciosListener implements ManejadorEvento {

    static final String CUENTA_SUSPENDIDA = "CuentaSuspendida";

    private final ComercioService comercioService;

    public EventosComerciosListener(ComercioService comercioService) {
        this.comercioService = comercioService;
    }

    @Override
    public Set<String> tiposSoportados() {
        return Set.of(CUENTA_SUSPENDIDA);
    }

    @Override
    public void manejar(DomainEvent evento) {
        // El agregado de CuentaSuspendida es la cuenta, que es el operadorId de sus comercios.
        comercioService.desactivarPorCuentaSuspendida(evento.agregadoIdLong());
    }
}
