package com.tamaka.common.event;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Component;

/**
 * Entrega cada evento entrante a los consumidores locales ({@link ManejadorEvento}) que declaran su tipo.
 * Es independiente del transporte: hoy lo invoca el endpoint HTTP interno; un listener SQS lo invocaria igual.
 * Los tipos sin consumidor se ignoran (un servicio puede recibir eventos que no le interesan).
 */
@Component
public class DespachadorEventos {

    private static final Logger log = LoggerFactory.getLogger(DespachadorEventos.class);

    private final Map<String, List<ManejadorEvento>> manejadoresPorTipo = new HashMap<>();
    private final EventosProperties properties;

    public DespachadorEventos(List<ManejadorEvento> manejadores, EventosProperties properties) {
        this.properties = properties;
        for (ManejadorEvento manejador : manejadores) {
            for (String tipo : manejador.tiposSoportados()) {
                manejadoresPorTipo.computeIfAbsent(tipo, t -> new ArrayList<>()).add(manejador);
            }
        }
    }

    /**
     * Verifica el secreto compartido (comparacion en tiempo constante) y despacha el evento.
     * Sin secreto configurado se rechaza todo (fail closed).
     */
    public int recibir(String secretoRecibido, DomainEvent evento) {
        if (!properties.tieneSecreto() || secretoRecibido == null
                || !MessageDigest.isEqual(properties.secreto().getBytes(StandardCharsets.UTF_8),
                        secretoRecibido.getBytes(StandardCharsets.UTF_8))) {
            throw new BadCredentialsException("Secreto de eventos invalido");
        }
        return despachar(evento);
    }

    /** Despacha a todos los consumidores del tipo. Devuelve cuantos lo procesaron. */
    public int despachar(DomainEvent evento) {
        List<ManejadorEvento> manejadores = manejadoresPorTipo.getOrDefault(evento.tipo(), List.of());
        if (manejadores.isEmpty()) {
            log.debug("evento sin consumidor local tipo={}", evento.tipo());
            return 0;
        }
        for (ManejadorEvento manejador : manejadores) {
            try {
                manejador.manejar(evento);
            } catch (DataIntegrityViolationException duplicado) {
                // Entrega "al menos una vez": si la primera entrega supera el timeout del productor pero se
                // procesa, el reintento llega en paralelo y choca con la clave unica de la proyeccion
                // (comprobar-e-insertar no es atomico). El efecto ya esta aplicado: se acepta como procesado
                // en vez de responder 409, que el relay del productor tomaria como rechazo definitivo.
                // Incluye DuplicateKeyException (MongoDB), que hereda de esta excepcion.
                log.info("evento ya aplicado (clave unica) tipo={} id={} consumidor={}",
                        evento.tipo(), evento.id(), manejador.getClass().getSimpleName());
            }
        }
        log.info("evento procesado tipo={} agregado={} consumidores={}",
                evento.tipo(), evento.agregadoId(), manejadores.size());
        return manejadores.size();
    }

    public boolean atiende(String tipo) {
        return manejadoresPorTipo.containsKey(tipo);
    }
}
