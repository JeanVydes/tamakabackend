package com.tamaka.common.event.outbox;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import com.tamaka.common.event.DomainEvent;

/**
 * Puerto de persistencia del outbox transaccional. Adaptadores: {@link JdbcOutboxStore} (servicios PostgreSQL,
 * participa en la transaccion del caso de uso) y {@link MongoOutboxStore} (servicios MongoDB).
 */
public interface OutboxStore {

    /** Guarda el evento en estado PENDIENTE (en la transaccion en curso, si la hay). */
    void guardar(DomainEvent evento);

    /**
     * Reclama hasta {@code limite} mensajes PENDIENTE cuyo proximo intento ya llego y los "arrienda" durante
     * {@code arriendo}: otra replica del relay no los tomara mientras tanto. Devuelve los mas antiguos primero.
     */
    List<MensajeOutbox> reclamarPendientes(Instant ahora, int limite, Duration arriendo);

    void marcarEnviado(String id, Instant ahora);

    /**
     * Registra un intento fallido. Con {@code definitivo=true} el mensaje pasa a FALLIDO y no se reintenta;
     * si no, sigue PENDIENTE hasta {@code proximoIntento}.
     */
    void registrarFallo(String id, int intentos, Instant proximoIntento, boolean definitivo, String error);
}
