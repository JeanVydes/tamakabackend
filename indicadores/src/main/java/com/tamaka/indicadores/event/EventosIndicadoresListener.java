package com.tamaka.indicadores.event;

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

import com.tamaka.common.event.DomainEvent;
import com.tamaka.common.event.ManejadorEvento;
import com.tamaka.indicadores.service.IndicadoresService;

/**
 * Consumidor de eventos de Indicadores (arquitectura sec. 5): ReservaSolicitada, PagoAprobado, ReservaCancelada.
 * Solo traduce el evento (nombre y referencia) y delega en la capa de servicio.
 * La referencia es siempre la reserva, para poder cruzar los tres hechos.
 */
@Component
public class EventosIndicadoresListener implements ManejadorEvento {

    static final String RESERVA_SOLICITADA = "ReservaSolicitada";
    static final String PAGO_APROBADO = "PagoAprobado";
    static final String RESERVA_CANCELADA = "ReservaCancelada";

    private static final Map<String, String> TIPO_HECHO = Map.of(
            RESERVA_SOLICITADA, "RESERVA_SOLICITADA",
            PAGO_APROBADO, "PAGO_APROBADO",
            RESERVA_CANCELADA, "RESERVA_CANCELADA");

    private static final Logger log = LoggerFactory.getLogger(EventosIndicadoresListener.class);

    private final IndicadoresService indicadoresService;

    public EventosIndicadoresListener(IndicadoresService indicadoresService) {
        this.indicadoresService = indicadoresService;
    }

    @Override
    public Set<String> tiposSoportados() {
        return TIPO_HECHO.keySet();
    }

    @Override
    public void manejar(DomainEvent evento) {
        Long reservaId = PAGO_APROBADO.equals(evento.tipo())
                ? evento.datoLongRequerido("reservaId")
                : evento.agregadoIdLong();
        try {
            indicadoresService.registrarHecho(TIPO_HECHO.get(evento.tipo()), evento.datoLong("operadorId"),
                    reservaId, evento.ocurridoEn());
        } catch (DataIntegrityViolationException duplicado) {
            // Entrega "al menos una vez": un reintento concurrente con la primera entrega choca con el indice
            // unico (tipo, referencia_id). El hecho ya esta registrado, asi que se acepta como procesado (2xx)
            // en vez de responder 409, que el relay del productor trataria como rechazo definitivo.
            log.info("hecho ya registrado tipo={} referencia={} evento={}", evento.tipo(), reservaId, evento.id());
        }
    }
}
