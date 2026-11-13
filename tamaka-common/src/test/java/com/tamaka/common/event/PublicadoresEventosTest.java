package com.tamaka.common.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.tamaka.common.event.http.HttpEventPublisher;

class PublicadoresEventosTest {

    /** Publicador de prueba que guarda lo publicado. */
    private static final class Captura implements EventPublisher {
        private final List<DomainEvent> publicados = new ArrayList<>();

        @Override
        public void publicar(DomainEvent evento) {
            publicados.add(evento);
        }
    }

    @Test
    void httpSinSuscriptoresDelegaEnElRespaldo() throws Exception {
        Captura respaldo = new Captura();
        EventosProperties properties = new EventosProperties("http", "secreto", Map.of(), Duration.ofSeconds(1), 0);

        try (HttpEventPublisher publicador = new HttpEventPublisher(properties, respaldo)) {
            publicador.publicar(DomainEvent.de("RolCambiado", 1L, Map.of()));
        }

        assertThat(respaldo.publicados).hasSize(1);
    }

    @Test
    void httpSinSecretoNoEnviaYDelegaEnElRespaldo() throws Exception {
        Captura respaldo = new Captura();
        EventosProperties properties = new EventosProperties("http", "",
                Map.of("RolCambiado", List.of("http://localhost:1/internal/eventos")), Duration.ofSeconds(1), 0);

        try (HttpEventPublisher publicador = new HttpEventPublisher(properties, respaldo)) {
            publicador.publicar(DomainEvent.de("RolCambiado", 1L, Map.of()));
        }

        assertThat(respaldo.publicados).hasSize(1);
    }

    @Test
    void sinTransaccionPublicaDeInmediato() {
        Captura destino = new Captura();
        new TransaccionalEventPublisher(destino).publicar(DomainEvent.de("X", 1L, Map.of()));

        assertThat(destino.publicados).hasSize(1);
    }

    @Test
    void conTransaccionEsperaAlCommit() {
        Captura destino = new Captura();
        TransactionSynchronizationManager.initSynchronization();
        try {
            new TransaccionalEventPublisher(destino).publicar(DomainEvent.de("X", 1L, Map.of()));
            assertThat(destino.publicados).isEmpty();

            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
            assertThat(destino.publicados).hasSize(1);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
