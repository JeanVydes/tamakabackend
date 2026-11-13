package com.tamaka.common.event;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.tamaka.common.event.http.HttpEventPublisher;
import com.tamaka.common.event.outbox.OutboxEventPublisher;
import com.tamaka.common.event.outbox.OutboxProperties;
import com.tamaka.common.event.outbox.OutboxStore;

/**
 * Seleccion del adaptador de eventos. Los servicios solo dependen de {@link EventPublisher}:
 * <ul>
 *   <li>{@code tamaka.eventos.outbox.habilitado=true}: el evento se guarda en el outbox (misma transaccion)
 *       y el relay lo entrega. Es el modo de los servicios que publican.</li>
 *   <li>si no, {@code tamaka.eventos.transporte}: "http" (entrega directa tras el commit) o "log".</li>
 * </ul>
 * Pasar a SNS/SQS es agregar otro {@code EntregadorEventos} y un listener SQS que invoque {@link DespachadorEventos}.
 */
@Configuration
@EnableConfigurationProperties({EventosProperties.class, OutboxProperties.class})
public class EventosConfig {

    /** Unico bean EventPublisher del contexto; si es AutoCloseable se cierra al apagar (destroyMethod inferido). */
    @Bean
    EventPublisher eventPublisher(EventosProperties properties, OutboxProperties outbox,
                                  ObjectProvider<OutboxStore> outboxStore) {
        if (outbox.habilitado()) {
            OutboxStore store = outboxStore.getIfAvailable();
            if (store == null) {
                throw new IllegalStateException("tamaka.eventos.outbox.habilitado=true pero no hay adaptador para el almacen '"
                        + outbox.almacen() + "' (falta spring-jdbc o spring-data-mongodb en el servicio)");
            }
            return new OutboxEventPublisher(store);
        }
        EventPublisher transporte = switch (properties.transporte()) {
            case "log" -> new LoggingEventPublisher();
            case "http" -> new HttpEventPublisher(properties, new LoggingEventPublisher());
            default -> throw new IllegalStateException(
                    "tamaka.eventos.transporte no soportado: " + properties.transporte());
        };
        return new TransaccionalEventPublisher(transporte);
    }
}
