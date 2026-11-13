package com.tamaka.common.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.BadCredentialsException;

class DespachadorEventosTest {

    private static final String SECRETO = "secreto-compartido-de-prueba";

    /** Consumidor de prueba que registra lo recibido. */
    private static final class Registro implements ManejadorEvento {
        private final List<DomainEvent> recibidos = new ArrayList<>();

        @Override
        public Set<String> tiposSoportados() {
            return Set.of("ReservaAceptada");
        }

        @Override
        public void manejar(DomainEvent evento) {
            recibidos.add(evento);
        }
    }

    private static EventosProperties propiedades(String secreto) {
        return new EventosProperties("http", secreto, Map.of(), Duration.ofSeconds(1), 0);
    }

    @Test
    void despachaSoloALosConsumidoresDelTipo() {
        Registro registro = new Registro();
        DespachadorEventos despachador = new DespachadorEventos(List.of(registro), propiedades(SECRETO));

        int atendidos = despachador.recibir(SECRETO, DomainEvent.de("ReservaAceptada", 5L, Map.of()));
        int ignorados = despachador.recibir(SECRETO, DomainEvent.de("EventoPublicado", 9L, Map.of()));

        assertThat(atendidos).isEqualTo(1);
        assertThat(ignorados).isZero();
        assertThat(registro.recibidos).extracting(DomainEvent::agregadoId).containsExactly("5");
    }

    @Test
    void rechazaSecretoInvalidoSinDespachar() {
        Registro registro = new Registro();
        DespachadorEventos despachador = new DespachadorEventos(List.of(registro), propiedades(SECRETO));

        assertThatThrownBy(() -> despachador.recibir("otro", DomainEvent.de("ReservaAceptada", 5L, Map.of())))
                .isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> despachador.recibir(null, DomainEvent.de("ReservaAceptada", 5L, Map.of())))
                .isInstanceOf(BadCredentialsException.class);
        assertThat(registro.recibidos).isEmpty();
    }

    @Test
    void reentregaQueChocaConClaveUnicaSeAceptaYSigueConLosDemasConsumidores() {
        ManejadorEvento duplicado = new ManejadorEvento() {
            @Override
            public Set<String> tiposSoportados() {
                return Set.of("ReservaAceptada");
            }

            @Override
            public void manejar(DomainEvent evento) {
                throw new DuplicateKeyException("reserva_pagable_pkey");
            }
        };
        Registro registro = new Registro();
        DespachadorEventos despachador = new DespachadorEventos(List.of(duplicado, registro), propiedades(SECRETO));

        int atendidos = despachador.recibir(SECRETO, DomainEvent.de("ReservaAceptada", 5L, Map.of()));

        assertThat(atendidos).isEqualTo(2);
        assertThat(registro.recibidos).hasSize(1);
    }

    @Test
    void sinSecretoConfiguradoRechazaTodo() {
        DespachadorEventos despachador = new DespachadorEventos(List.of(new Registro()), propiedades(""));

        assertThatThrownBy(() -> despachador.recibir("", DomainEvent.de("ReservaAceptada", 5L, Map.of())))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void leeDatosTipadosComoLlegarianDesdeJson() {
        // Tras JSON los ids llegan como Integer, los montos y fechas como texto.
        DomainEvent evento = new DomainEvent("ReservaAceptada", "5", Instant.now(), Map.of(
                "turistaId", 7,
                "valorAPagar", "80000.50",
                "plazoPago", "2026-11-09T11:00:00Z",
                "categorias", List.of("PLAYA", "CULTURA")));

        assertThat(evento.agregadoIdLong()).isEqualTo(5L);
        assertThat(evento.datoLongRequerido("turistaId")).isEqualTo(7L);
        assertThat(evento.datoDecimalRequerido("valorAPagar")).isEqualByComparingTo(new BigDecimal("80000.50"));
        assertThat(evento.datoInstanteRequerido("plazoPago")).isEqualTo(Instant.parse("2026-11-09T11:00:00Z"));
        assertThat(evento.datoListaTexto("categorias")).containsExactly("PLAYA", "CULTURA");
        assertThat(evento.datoLong("operadorId")).isNull();
        assertThatThrownBy(() -> evento.datoLongRequerido("operadorId"))
                .isInstanceOf(EventoInvalidoException.class);
    }

    @Test
    void suscriptoresSeBuscanSinDistinguirMayusculas() {
        EventosProperties properties = new EventosProperties(null, SECRETO,
                Map.of("reservaaceptada", List.of(" http://localhost:8086/internal/eventos ", "")), null, null);

        assertThat(properties.transporte()).isEqualTo("http");
        assertThat(properties.suscriptoresDe("ReservaAceptada"))
                .containsExactly("http://localhost:8086/internal/eventos");
        assertThat(properties.reintentos()).isEqualTo(3);
    }
}
