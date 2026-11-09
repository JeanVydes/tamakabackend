package com.tamaka.common.event;

import java.time.Instant;
import java.util.Map;

/**
 * Evento de dominio publicado por un microservicio hacia el bus (SNS/SQS).
 * El payload contiene solo identificadores y datos minimos (Ley 1581: minimizacion de datos personales).
 *
 * @param tipo       nombre del evento, p. ej. "ReservaAceptada"
 * @param agregadoId identificador del agregado afectado
 * @param ocurridoEn instante de la ocurrencia
 * @param datos      datos minimos del evento
 */
public record DomainEvent(
        String tipo,
        String agregadoId,
        Instant ocurridoEn,
        Map<String, Object> datos
) {

    public DomainEvent {
        datos = datos == null ? Map.of() : Map.copyOf(datos);
        ocurridoEn = ocurridoEn == null ? Instant.now() : ocurridoEn;
    }

    public static DomainEvent de(String tipo, Object agregadoId, Map<String, Object> datos) {
        return new DomainEvent(tipo, String.valueOf(agregadoId), Instant.now(), datos);
    }
}
