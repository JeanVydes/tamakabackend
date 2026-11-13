package com.tamaka.notificaciones.event;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.tamaka.common.event.DomainEvent;
import com.tamaka.common.event.ManejadorEvento;
import com.tamaka.notificaciones.service.NotificacionService;

/**
 * Mantiene los modelos de lectura propios de Notificaciones a partir de eventos de Preferencias:
 * idioma de cada usuario y seguidores de cada atractivo. Solo traduce el evento y delega.
 */
@Component
public class ProyeccionesPreferenciasListener implements ManejadorEvento {

    static final String PREFERENCIAS_ACTUALIZADAS = "PreferenciasActualizadas";
    static final String FAVORITO_GUARDADO = "FavoritoGuardado";
    static final String FAVORITO_ELIMINADO = "FavoritoEliminado";

    private final NotificacionService notificacionService;

    public ProyeccionesPreferenciasListener(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    @Override
    public Set<String> tiposSoportados() {
        return Set.of(PREFERENCIAS_ACTUALIZADAS, FAVORITO_GUARDADO, FAVORITO_ELIMINADO);
    }

    @Override
    public void manejar(DomainEvent evento) {
        switch (evento.tipo()) {
            case PREFERENCIAS_ACTUALIZADAS -> notificacionService.registrarIdioma(
                    evento.agregadoIdLong(), evento.datoTexto("idioma"));
            case FAVORITO_GUARDADO -> notificacionService.registrarSeguimiento(
                    evento.datoLongRequerido("turistaId"), evento.datoTexto("tipo"), evento.datoTexto("refId"));
            case FAVORITO_ELIMINADO -> notificacionService.eliminarSeguimiento(
                    evento.datoLongRequerido("turistaId"), evento.datoTexto("tipo"), evento.datoTexto("refId"));
            default -> {
                // Tipo no atendido: el despachador solo entrega los declarados.
            }
        }
    }
}
