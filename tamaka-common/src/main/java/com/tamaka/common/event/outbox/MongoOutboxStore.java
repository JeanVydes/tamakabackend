package com.tamaka.common.event.outbox;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.tamaka.common.event.DomainEvent;

/**
 * Adaptador MongoDB del outbox (Preferencias). Sin transacciones multi-documento, el evento se guarda justo
 * despues del documento de negocio: si el proceso cae entre ambos se pierde ese evento (limitacion conocida),
 * pero una vez guardado sobrevive a caidas y lo entrega el relay.
 * El reclamo es atomico por documento ({@code findAndModify}) con arriendo, apto para varias replicas.
 */
public class MongoOutboxStore implements OutboxStore {

    private final MongoTemplate mongo;
    private final String coleccion;

    public MongoOutboxStore(MongoTemplate mongo, String coleccion) {
        this.mongo = mongo;
        this.coleccion = coleccion;
    }

    @Override
    public void guardar(DomainEvent evento) {
        Instant ahora = Instant.now();
        mongo.insert(new OutboxDocumento(evento.id(), evento.tipo(), evento.agregadoId(),
                EventoJson.escribir(evento), ahora), coleccion);
    }

    @Override
    public List<MensajeOutbox> reclamarPendientes(Instant ahora, int limite, Duration arriendo) {
        List<MensajeOutbox> reclamados = new ArrayList<>();
        Query query = new Query(Criteria.where("estado").is(EstadoOutbox.PENDIENTE.name())
                .and("proximoIntento").lte(ahora))
                .with(Sort.by(Sort.Direction.ASC, "creadoEn"));
        Update arrendar = new Update().set("proximoIntento", ahora.plus(arriendo));
        for (int i = 0; i < limite; i++) {
            OutboxDocumento documento = mongo.findAndModify(query, arrendar,
                    FindAndModifyOptions.options().returnNew(true), OutboxDocumento.class, coleccion);
            if (documento == null) {
                break;
            }
            reclamados.add(new MensajeOutbox(documento.getId(), EventoJson.leer(documento.getPayload()),
                    documento.getIntentos(), documento.getCreadoEn()));
        }
        return reclamados;
    }

    @Override
    public void marcarEnviado(String id, Instant ahora) {
        mongo.updateFirst(Query.query(Criteria.where("_id").is(id)),
                new Update().set("estado", EstadoOutbox.ENVIADO.name()).set("enviadoEn", ahora).unset("ultimoError"),
                OutboxDocumento.class, coleccion);
    }

    @Override
    public void registrarFallo(String id, int intentos, Instant proximoIntento, boolean definitivo, String error) {
        mongo.updateFirst(Query.query(Criteria.where("_id").is(id)),
                new Update()
                        .set("estado", definitivo ? EstadoOutbox.FALLIDO.name() : EstadoOutbox.PENDIENTE.name())
                        .set("intentos", intentos)
                        .set("proximoIntento", proximoIntento)
                        .set("ultimoError", error),
                OutboxDocumento.class, coleccion);
    }
}
