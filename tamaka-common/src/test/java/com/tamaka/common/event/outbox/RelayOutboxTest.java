package com.tamaka.common.event.outbox;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.tamaka.common.event.DomainEvent;
import com.tamaka.common.event.EntregadorEventos;

class RelayOutboxTest {

    private static final Instant AHORA = Instant.parse("2026-11-09T10:00:00Z");

    /** Almacen en memoria con la misma semantica que los adaptadores reales. */
    static final class OutboxEnMemoria implements OutboxStore {

        static final class Fila {
            final DomainEvent evento;
            EstadoOutbox estado = EstadoOutbox.PENDIENTE;
            int intentos;
            Instant proximoIntento;
            final Instant creadoEn;
            String error;

            Fila(DomainEvent evento, Instant ahora) {
                this.evento = evento;
                this.proximoIntento = ahora;
                this.creadoEn = ahora;
            }
        }

        final Map<String, Fila> filas = new LinkedHashMap<>();

        @Override
        public void guardar(DomainEvent evento) {
            filas.put(evento.id(), new Fila(evento, AHORA));
        }

        @Override
        public List<MensajeOutbox> reclamarPendientes(Instant ahora, int limite, Duration arriendo) {
            List<MensajeOutbox> reclamados = new ArrayList<>();
            for (Fila fila : filas.values()) {
                if (reclamados.size() < limite && fila.estado == EstadoOutbox.PENDIENTE
                        && !fila.proximoIntento.isAfter(ahora)) {
                    fila.proximoIntento = ahora.plus(arriendo);
                    reclamados.add(new MensajeOutbox(fila.evento.id(), fila.evento, fila.intentos, fila.creadoEn));
                }
            }
            return reclamados;
        }

        @Override
        public void marcarEnviado(String id, Instant ahora) {
            filas.get(id).estado = EstadoOutbox.ENVIADO;
        }

        @Override
        public void registrarFallo(String id, int intentos, Instant proximoIntento, boolean definitivo, String error) {
            Fila fila = filas.get(id);
            fila.intentos = intentos;
            fila.proximoIntento = proximoIntento;
            fila.error = error;
            fila.estado = definitivo ? EstadoOutbox.FALLIDO : EstadoOutbox.PENDIENTE;
        }

        Fila unica() {
            return filas.values().iterator().next();
        }
    }

    /** Entregador que responde segun una secuencia programada. */
    static final class EntregadorProgramado implements EntregadorEventos {
        final List<Resultado> respuestas;
        int llamadas;
        boolean disponible = true;

        EntregadorProgramado(Resultado... respuestas) {
            this.respuestas = List.of(respuestas);
        }

        @Override
        public boolean disponible() {
            return disponible;
        }

        @Override
        public Resultado entregar(DomainEvent evento) {
            Resultado resultado = respuestas.get(Math.min(llamadas, respuestas.size() - 1));
            llamadas++;
            return resultado;
        }
    }

    private static OutboxProperties propiedades(int maxIntentos) {
        return new OutboxProperties(true, "jdbc", "x.outbox_evento", null, 10, Duration.ofSeconds(2),
                Duration.ofMinutes(1), Duration.ofSeconds(2), Duration.ofMinutes(10), maxIntentos);
    }

    private static RelayOutbox relay(OutboxStore store, EntregadorEventos entregador, int maxIntentos, Instant ahora) {
        return new RelayOutbox(store, entregador, propiedades(maxIntentos), Clock.fixed(ahora, ZoneOffset.UTC));
    }

    @Test
    void elPublicadorSoloGuardaEnElOutbox() {
        OutboxEnMemoria store = new OutboxEnMemoria();
        DomainEvent evento = DomainEvent.de("ReservaAceptada", 5L, Map.of("turistaId", 1L));

        new OutboxEventPublisher(store).publicar(evento);

        assertThat(store.filas).containsOnlyKeys(evento.id());
        assertThat(store.unica().estado).isEqualTo(EstadoOutbox.PENDIENTE);
    }

    @Test
    void entregaExitosaMarcaEnviado() {
        OutboxEnMemoria store = new OutboxEnMemoria();
        store.guardar(DomainEvent.de("ReservaAceptada", 5L, Map.of()));

        int enviados = relay(store, new EntregadorProgramado(EntregadorEventos.Resultado.ENTREGADO), 5, AHORA)
                .procesarPendientes();

        assertThat(enviados).isEqualTo(1);
        assertThat(store.unica().estado).isEqualTo(EstadoOutbox.ENVIADO);
    }

    @Test
    void falloTransitorioReintentaConEsperaExponencial() {
        OutboxEnMemoria store = new OutboxEnMemoria();
        store.guardar(DomainEvent.de("ReservaAceptada", 5L, Map.of()));
        EntregadorProgramado entregador = new EntregadorProgramado(EntregadorEventos.Resultado.REINTENTAR);

        relay(store, entregador, 5, AHORA).procesarPendientes();
        assertThat(store.unica().estado).isEqualTo(EstadoOutbox.PENDIENTE);
        assertThat(store.unica().intentos).isEqualTo(1);
        assertThat(store.unica().proximoIntento).isEqualTo(AHORA.plusSeconds(2));

        // Antes de la espera no se reintenta.
        relay(store, entregador, 5, AHORA.plusSeconds(1)).procesarPendientes();
        assertThat(entregador.llamadas).isEqualTo(1);

        relay(store, entregador, 5, AHORA.plusSeconds(2)).procesarPendientes();
        assertThat(store.unica().intentos).isEqualTo(2);
        assertThat(store.unica().proximoIntento).isEqualTo(AHORA.plusSeconds(2 + 4));
    }

    @Test
    void trasMaxIntentosQuedaFallido() {
        OutboxEnMemoria store = new OutboxEnMemoria();
        store.guardar(DomainEvent.de("ReservaAceptada", 5L, Map.of()));
        EntregadorProgramado entregador = new EntregadorProgramado(EntregadorEventos.Resultado.REINTENTAR);

        Instant momento = AHORA;
        for (int i = 0; i < 5; i++) {
            relay(store, entregador, 3, momento).procesarPendientes();
            momento = momento.plus(Duration.ofHours(1));
        }

        assertThat(store.unica().estado).isEqualTo(EstadoOutbox.FALLIDO);
        assertThat(store.unica().intentos).isEqualTo(3);
        assertThat(entregador.llamadas).isEqualTo(3);
    }

    @Test
    void rechazoDefinitivoQuedaFallidoSinReintentar() {
        OutboxEnMemoria store = new OutboxEnMemoria();
        store.guardar(DomainEvent.de("ReservaAceptada", 5L, Map.of()));

        relay(store, new EntregadorProgramado(EntregadorEventos.Resultado.RECHAZADO), 10, AHORA).procesarPendientes();

        assertThat(store.unica().estado).isEqualTo(EstadoOutbox.FALLIDO);
        assertThat(store.unica().intentos).isEqualTo(1);
    }

    @Test
    void sinTransporteDisponibleNoConsumeIntentos() {
        OutboxEnMemoria store = new OutboxEnMemoria();
        store.guardar(DomainEvent.de("ReservaAceptada", 5L, Map.of()));
        EntregadorProgramado entregador = new EntregadorProgramado(EntregadorEventos.Resultado.ENTREGADO);
        entregador.disponible = false;

        relay(store, entregador, 3, AHORA).procesarPendientes();

        assertThat(entregador.llamadas).isZero();
        assertThat(store.unica().estado).isEqualTo(EstadoOutbox.PENDIENTE);
        assertThat(store.unica().intentos).isZero();
    }

    @Test
    void laEsperaTieneTope() {
        OutboxProperties properties = propiedades(50);

        assertThat(properties.esperaTras(1)).isEqualTo(Duration.ofSeconds(2));
        assertThat(properties.esperaTras(3)).isEqualTo(Duration.ofSeconds(8));
        assertThat(properties.esperaTras(30)).isEqualTo(Duration.ofMinutes(10));
    }

    @Test
    void elEventoConservaSuIdAlSerializarse() {
        DomainEvent original = DomainEvent.de("ReservaAceptada", 5L, Map.of(
                "turistaId", 1L, "valorAPagar", "80000.50", "categorias", List.of("PLAYA")));

        DomainEvent leido = EventoJson.leer(EventoJson.escribir(original));

        assertThat(leido.id()).isEqualTo(original.id());
        assertThat(leido.tipo()).isEqualTo("ReservaAceptada");
        assertThat(leido.ocurridoEn()).isEqualTo(original.ocurridoEn());
        assertThat(leido.datoLongRequerido("turistaId")).isEqualTo(1L);
        assertThat(leido.datoListaTexto("categorias")).containsExactly("PLAYA");
    }
}
