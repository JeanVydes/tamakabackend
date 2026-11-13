package com.tamaka.common.event.outbox;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.jdbc.core.JdbcTemplate;

import com.tamaka.common.event.DomainEvent;

/**
 * Adaptador PostgreSQL del outbox. Usa JdbcTemplate sobre el mismo DataSource que JPA, de modo que el INSERT
 * participa en la transaccion del caso de uso (JpaTransactionManager expone la conexion a JDBC).
 * La tabla la crea la migracion Flyway de cada servicio y se indica calificada con el esquema.
 * El reclamo usa {@code FOR UPDATE SKIP LOCKED} + arriendo: varias replicas del relay no toman el mismo mensaje.
 */
public class JdbcOutboxStore implements OutboxStore {

    private static final Pattern TABLA_VALIDA = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)?");

    private final JdbcTemplate jdbc;
    private final String tabla;

    public JdbcOutboxStore(JdbcTemplate jdbc, String tabla) {
        if (!TABLA_VALIDA.matcher(tabla).matches()) {
            throw new IllegalArgumentException("Nombre de tabla de outbox invalido: " + tabla);
        }
        this.jdbc = jdbc;
        this.tabla = tabla;
    }

    @Override
    public void guardar(DomainEvent evento) {
        Timestamp ahora = Timestamp.from(Instant.now());
        jdbc.update("INSERT INTO " + tabla
                        + " (id, tipo, agregado_id, payload, estado, intentos, proximo_intento, creado_en)"
                        + " VALUES (?, ?, ?, ?, 'PENDIENTE', 0, ?, ?)",
                evento.id(), evento.tipo(), evento.agregadoId(), EventoJson.escribir(evento), ahora, ahora);
    }

    @Override
    public List<MensajeOutbox> reclamarPendientes(Instant ahora, int limite, Duration arriendo) {
        String sql = "UPDATE " + tabla + " SET proximo_intento = ?"
                + " WHERE id IN (SELECT id FROM " + tabla
                + " WHERE estado = 'PENDIENTE' AND proximo_intento <= ?"
                + " ORDER BY creado_en LIMIT ? FOR UPDATE SKIP LOCKED)"
                + " RETURNING id, payload, intentos, creado_en";
        return jdbc.query(sql,
                (rs, fila) -> new MensajeOutbox(
                        rs.getString("id"),
                        EventoJson.leer(rs.getString("payload")),
                        rs.getInt("intentos"),
                        rs.getTimestamp("creado_en").toInstant()),
                Timestamp.from(ahora.plus(arriendo)), Timestamp.from(ahora), limite);
    }

    @Override
    public void marcarEnviado(String id, Instant ahora) {
        jdbc.update("UPDATE " + tabla + " SET estado = 'ENVIADO', enviado_en = ?, ultimo_error = NULL WHERE id = ?",
                Timestamp.from(ahora), id);
    }

    @Override
    public void registrarFallo(String id, int intentos, Instant proximoIntento, boolean definitivo, String error) {
        jdbc.update("UPDATE " + tabla + " SET estado = ?, intentos = ?, proximo_intento = ?, ultimo_error = ? WHERE id = ?",
                definitivo ? EstadoOutbox.FALLIDO.name() : EstadoOutbox.PENDIENTE.name(),
                intentos, Timestamp.from(proximoIntento), error, id);
    }
}
