package com.tamaka.common.event.outbox;

import java.time.Clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.tamaka.common.event.EventosProperties;
import com.tamaka.common.event.http.EntregadorHttp;

/**
 * Activa el outbox solo en los servicios que publican eventos ({@code tamaka.eventos.outbox.habilitado=true}).
 * El adaptador de almacenamiento se elige con {@code tamaka.eventos.outbox.almacen} (jdbc | mongo); cada uno se
 * carga solo si su libreria esta en el classpath del servicio.
 */
@Configuration
@EnableScheduling
@Import({OutboxConfig.Jdbc.class, OutboxConfig.Mongo.class})
@ConditionalOnProperty(prefix = "tamaka.eventos.outbox", name = "habilitado", havingValue = "true")
public class OutboxConfig {

    @Bean
    RelayOutbox relayOutbox(OutboxStore store, EventosProperties eventos, OutboxProperties outbox) {
        return new RelayOutbox(store, new EntregadorHttp(eventos), outbox, Clock.systemUTC());
    }

    /** Importada (no escaneada): solo se evalua si el outbox esta habilitado. */
    @ConditionalOnClass(name = "org.springframework.jdbc.core.JdbcTemplate")
    @ConditionalOnProperty(prefix = "tamaka.eventos.outbox", name = "almacen", havingValue = "jdbc", matchIfMissing = true)
    static class Jdbc {

        @Bean
        OutboxStore outboxStore(JdbcTemplate jdbcTemplate, OutboxProperties outbox) {
            return new JdbcOutboxStore(jdbcTemplate, outbox.tabla());
        }
    }

    @ConditionalOnClass(name = "org.springframework.data.mongodb.core.MongoTemplate")
    @ConditionalOnProperty(prefix = "tamaka.eventos.outbox", name = "almacen", havingValue = "mongo")
    static class Mongo {

        @Bean
        OutboxStore outboxStore(MongoTemplate mongoTemplate, OutboxProperties outbox) {
            return new MongoOutboxStore(mongoTemplate, outbox.coleccion());
        }
    }
}
