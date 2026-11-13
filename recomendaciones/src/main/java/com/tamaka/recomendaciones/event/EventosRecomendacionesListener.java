package com.tamaka.recomendaciones.event;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.tamaka.common.event.DomainEvent;
import com.tamaka.common.event.ManejadorEvento;
import com.tamaka.recomendaciones.service.RecomendacionService;

/**
 * Consumidor de eventos de Recomendaciones (arquitectura sec. 5): EventoPublicado, AtractivoActualizado y
 * PreferenciasActualizadas; ademas EventoDesactivado para no recomendar eventos retirados y los eventos de
 * servicios de Comercios (ServicioCreado/ServicioActualizado los registran, ServicioDesactivado los retira).
 * Todas las operaciones son idempotentes (conjuntos de Redis): una reentrega no duplica nada.
 * Solo traduce el evento y delega en la capa de servicio.
 */
@Component
public class EventosRecomendacionesListener implements ManejadorEvento {

    static final String EVENTO_PUBLICADO = "EventoPublicado";
    static final String EVENTO_DESACTIVADO = "EventoDesactivado";
    static final String ATRACTIVO_ACTUALIZADO = "AtractivoActualizado";
    static final String PREFERENCIAS_ACTUALIZADAS = "PreferenciasActualizadas";
    static final String SERVICIO_CREADO = "ServicioCreado";
    static final String SERVICIO_ACTUALIZADO = "ServicioActualizado";
    static final String SERVICIO_DESACTIVADO = "ServicioDesactivado";
    private static final String TIPO_EVENTO = "EVENTO";
    private static final String TIPO_SERVICIO = "SERVICIO";

    private final RecomendacionService recomendacionService;

    public EventosRecomendacionesListener(RecomendacionService recomendacionService) {
        this.recomendacionService = recomendacionService;
    }

    @Override
    public Set<String> tiposSoportados() {
        return Set.of(EVENTO_PUBLICADO, EVENTO_DESACTIVADO, ATRACTIVO_ACTUALIZADO, PREFERENCIAS_ACTUALIZADAS,
                SERVICIO_CREADO, SERVICIO_ACTUALIZADO, SERVICIO_DESACTIVADO);
    }

    @Override
    public void manejar(DomainEvent evento) {
        switch (evento.tipo()) {
            case EVENTO_PUBLICADO -> recomendacionService.registrarItem(
                    TIPO_EVENTO, evento.agregadoIdLong(), evento.datoTexto("categoria"));
            case EVENTO_DESACTIVADO -> recomendacionService.retirarItem(TIPO_EVENTO, evento.agregadoIdLong());
            case ATRACTIVO_ACTUALIZADO -> recomendacionService.registrarAtractivo(
                    evento.agregadoIdLong(), evento.datoTexto("categoria"), evento.datoTexto("estado"));
            case PREFERENCIAS_ACTUALIZADAS -> recomendacionService.registrarPreferencias(
                    evento.agregadoIdLong(), evento.datoListaTexto("categorias"));
            case SERVICIO_CREADO, SERVICIO_ACTUALIZADO -> recomendacionService.registrarItem(
                    TIPO_SERVICIO, evento.agregadoIdLong(), evento.datoTexto("categoria"));
            case SERVICIO_DESACTIVADO -> recomendacionService.retirarItem(TIPO_SERVICIO, evento.agregadoIdLong());
            default -> {
                // Tipo no atendido: el despachador solo entrega los declarados.
            }
        }
    }
}
