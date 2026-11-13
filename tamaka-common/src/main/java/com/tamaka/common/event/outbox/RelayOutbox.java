package com.tamaka.common.event.outbox;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import com.tamaka.common.event.EntregadorEventos;

/**
 * Relay del outbox: reclama los mensajes pendientes, los entrega con el transporte configurado y registra el
 * resultado. Fallos transitorios: reintento con espera exponencial; tras {@code maxIntentos}, o ante un rechazo
 * definitivo (4xx), el mensaje queda FALLIDO para revision manual.
 * Garantia: "al menos una vez". Un mensaje puede reentregarse (p. ej., si un suscriptor fallo y otro no), por eso
 * los consumidores son idempotentes por id de evento.
 */
public class RelayOutbox {

    private static final Logger log = LoggerFactory.getLogger(RelayOutbox.class);
    private static final int MAX_LARGO_ERROR = 500;

    private final OutboxStore store;
    private final EntregadorEventos entregador;
    private final OutboxProperties properties;
    private final Clock reloj;
    private volatile boolean avisoSinTransporte;

    public RelayOutbox(OutboxStore store, EntregadorEventos entregador, OutboxProperties properties, Clock reloj) {
        this.store = store;
        this.entregador = entregador;
        this.properties = properties;
        this.reloj = reloj;
    }

    @Scheduled(fixedDelayString = "${tamaka.eventos.outbox.intervalo:PT2S}",
            initialDelayString = "${tamaka.eventos.outbox.intervalo:PT2S}")
    public void ejecutar() {
        try {
            procesarPendientes();
        } catch (RuntimeException ex) {
            // Un fallo de la base no debe detener el scheduler; se reintenta en el siguiente ciclo.
            log.error("relay del outbox fallo: {}", ex.getClass().getSimpleName(), ex);
        }
    }

    /** Un ciclo del relay. Devuelve cuantos mensajes quedaron ENVIADO. */
    public int procesarPendientes() {
        if (!entregador.disponible()) {
            if (!avisoSinTransporte) {
                log.warn("outbox sin transporte disponible (falta tamaka.eventos.secreto): los eventos quedan PENDIENTE");
                avisoSinTransporte = true;
            }
            return 0;
        }
        avisoSinTransporte = false;
        Instant ahora = reloj.instant();
        List<MensajeOutbox> mensajes = store.reclamarPendientes(ahora, properties.lote(), properties.arriendo())
                .stream()
                .sorted(Comparator.comparing(MensajeOutbox::creadoEn))
                .toList();
        int enviados = 0;
        for (MensajeOutbox mensaje : mensajes) {
            if (procesar(mensaje)) {
                enviados++;
            }
        }
        return enviados;
    }

    private boolean procesar(MensajeOutbox mensaje) {
        EntregadorEventos.Resultado resultado;
        String error;
        try {
            resultado = entregador.entregar(mensaje.evento());
            error = resultado.name();
        } catch (RuntimeException ex) {
            resultado = EntregadorEventos.Resultado.REINTENTAR;
            error = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        }
        Instant ahora = reloj.instant();
        if (resultado == EntregadorEventos.Resultado.ENTREGADO) {
            store.marcarEnviado(mensaje.id(), ahora);
            return true;
        }
        int intentos = mensaje.intentos() + 1;
        boolean definitivo = resultado == EntregadorEventos.Resultado.RECHAZADO || intentos >= properties.maxIntentos();
        store.registrarFallo(mensaje.id(), intentos, ahora.plus(properties.esperaTras(intentos)), definitivo,
                recortar(error));
        if (definitivo) {
            log.error("evento FALLIDO tipo={} id={} intentos={} causa={}",
                    mensaje.evento().tipo(), mensaje.id(), intentos, error);
        }
        return false;
    }

    private static String recortar(String texto) {
        if (texto == null) {
            return null;
        }
        return texto.length() <= MAX_LARGO_ERROR ? texto : texto.substring(0, MAX_LARGO_ERROR);
    }
}
