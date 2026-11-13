package com.tamaka.common.event.http;

import java.net.http.HttpClient;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.tamaka.common.event.DomainEvent;
import com.tamaka.common.event.EntregadorEventos;
import com.tamaka.common.event.EventosProperties;

/**
 * Entrega HTTP sincrona: {@code POST <url>} con el evento en JSON y el secreto compartido en la cabecera.
 * No reintenta: quien la usa (relay del outbox o {@link HttpEventPublisher}) decide la politica.
 */
public class EntregadorHttp implements EntregadorEventos {

    private static final Logger log = LoggerFactory.getLogger(EntregadorHttp.class);

    /** Resultado de una entrega a un unico suscriptor. */
    public enum ResultadoDestino { OK, TRANSITORIO, DEFINITIVO }

    private final EventosProperties properties;
    private volatile RestClient restClient;

    public EntregadorHttp(EventosProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean disponible() {
        return properties.tieneSecreto();
    }

    @Override
    public Resultado entregar(DomainEvent evento) {
        List<String> destinos = properties.suscriptoresDe(evento.tipo());
        boolean transitorio = false;
        boolean definitivo = false;
        for (String destino : destinos) {
            switch (entregarA(destino, evento)) {
                case TRANSITORIO -> transitorio = true;
                case DEFINITIVO -> definitivo = true;
                case OK -> {
                    // entregado a este suscriptor
                }
            }
        }
        if (transitorio) {
            return Resultado.REINTENTAR;
        }
        return definitivo ? Resultado.RECHAZADO : Resultado.ENTREGADO;
    }

    public ResultadoDestino entregarA(String destino, DomainEvent evento) {
        try {
            cliente().post()
                    .uri(destino)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpEventPublisher.CABECERA_SECRETO, properties.secreto())
                    .body(evento)
                    .retrieve()
                    .toBodilessEntity();
            log.info("evento entregado tipo={} id={} destino={}", evento.tipo(), evento.id(), destino);
            return ResultadoDestino.OK;
        } catch (HttpClientErrorException ex) {
            log.error("evento rechazado tipo={} id={} destino={} status={}",
                    evento.tipo(), evento.id(), destino, ex.getStatusCode().value());
            return ResultadoDestino.DEFINITIVO;
        } catch (RestClientException ex) {
            log.warn("fallo transitorio tipo={} id={} destino={}: {}",
                    evento.tipo(), evento.id(), destino, ex.getClass().getSimpleName());
            return ResultadoDestino.TRANSITORIO;
        }
    }

    /** El cliente HTTP se crea al primer envio: un servicio que no publica no abre conexiones. */
    private RestClient cliente() {
        RestClient actual = restClient;
        if (actual == null) {
            synchronized (this) {
                if (restClient == null) {
                    HttpClient httpClient = HttpClient.newBuilder().connectTimeout(properties.timeout()).build();
                    JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(httpClient);
                    fabrica.setReadTimeout(properties.timeout());
                    restClient = RestClient.builder().requestFactory(fabrica).build();
                }
                actual = restClient;
            }
        }
        return actual;
    }
}
