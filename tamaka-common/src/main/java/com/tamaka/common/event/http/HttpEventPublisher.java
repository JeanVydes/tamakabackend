package com.tamaka.common.event.http;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tamaka.common.event.DomainEvent;
import com.tamaka.common.event.EventPublisher;
import com.tamaka.common.event.EventosProperties;

/**
 * Publicacion HTTP directa (sin outbox): entrega el evento con {@code POST} a cada URL suscrita al tipo
 * ({@code tamaka.eventos.suscriptores.<Tipo>}), de forma asincrona (hilos virtuales) y con reintentos con espera
 * exponencial ante errores transitorios. Un 4xx no se reintenta.
 * Si el tipo no tiene suscriptores, o falta el secreto, delega en el respaldo (log).
 * Es el modo de los servicios sin base transaccional propia; los que la tienen usan el outbox
 * ({@code tamaka.eventos.outbox.habilitado=true}), que sobrevive a caidas.
 */
public class HttpEventPublisher implements EventPublisher, AutoCloseable {

    public static final String CABECERA_SECRETO = "X-Tamaka-Eventos-Secreto";
    public static final String RUTA_INTERNA = "/internal/eventos";

    private static final Logger log = LoggerFactory.getLogger(HttpEventPublisher.class);
    private static final long ESPERA_BASE_MS = 250;

    private final EventosProperties properties;
    private final EventPublisher respaldo;
    private final EntregadorHttp entregador;
    private final ExecutorService executor;

    public HttpEventPublisher(EventosProperties properties, EventPublisher respaldo) {
        this.properties = properties;
        this.respaldo = respaldo;
        this.entregador = new EntregadorHttp(properties);
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    public void publicar(DomainEvent evento) {
        List<String> destinos = properties.suscriptoresDe(evento.tipo());
        if (destinos.isEmpty()) {
            respaldo.publicar(evento);
            return;
        }
        if (!properties.tieneSecreto()) {
            log.warn("tamaka.eventos.secreto vacio: el evento {} solo se registra en el log", evento.tipo());
            respaldo.publicar(evento);
            return;
        }
        for (String destino : destinos) {
            executor.execute(() -> entregarConReintentos(destino, evento));
        }
    }

    boolean entregarConReintentos(String destino, DomainEvent evento) {
        int intentos = properties.reintentos() + 1;
        for (int intento = 1; intento <= intentos; intento++) {
            switch (entregador.entregarA(destino, evento)) {
                case OK -> {
                    return true;
                }
                case DEFINITIVO -> {
                    return false;
                }
                case TRANSITORIO -> {
                    if (intento < intentos && !esperar(ESPERA_BASE_MS << (intento - 1))) {
                        return false;
                    }
                }
            }
        }
        log.error("evento perdido tras agotar reintentos tipo={} id={} destino={}", evento.tipo(), evento.id(), destino);
        return false;
    }

    private static boolean esperar(long milisegundos) {
        try {
            Thread.sleep(milisegundos);
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void close() throws InterruptedException {
        executor.shutdown();
        if (!executor.awaitTermination(properties.timeout().toSeconds() + 1, TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }
    }
}
