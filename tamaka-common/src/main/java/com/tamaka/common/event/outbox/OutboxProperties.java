package com.tamaka.common.event.outbox;

import java.time.Duration;
import java.util.Locale;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracion del outbox transaccional ({@code tamaka.eventos.outbox.*}).
 *
 * @param habilitado   true: los eventos se guardan en el outbox y un relay los entrega (servicios que publican)
 * @param almacen      "jdbc" (PostgreSQL) o "mongo"
 * @param tabla        tabla JDBC calificada con el esquema, p. ej. "reservas.outbox_evento"
 * @param coleccion    coleccion MongoDB
 * @param lote         mensajes por ciclo del relay
 * @param intervalo    pausa entre ciclos del relay
 * @param arriendo     tiempo durante el cual un mensaje reclamado no lo toma otra replica
 * @param esperaBase   espera tras el primer fallo; se duplica en cada intento
 * @param esperaMaxima tope de la espera entre intentos
 * @param maxIntentos  intentos antes de marcar el mensaje FALLIDO
 */
@ConfigurationProperties(prefix = "tamaka.eventos.outbox")
public record OutboxProperties(
        Boolean habilitado,
        String almacen,
        String tabla,
        String coleccion,
        Integer lote,
        Duration intervalo,
        Duration arriendo,
        Duration esperaBase,
        Duration esperaMaxima,
        Integer maxIntentos
) {

    public OutboxProperties {
        habilitado = habilitado != null && habilitado;
        almacen = almacen == null || almacen.isBlank() ? "jdbc" : almacen.trim().toLowerCase(Locale.ROOT);
        tabla = tabla == null || tabla.isBlank() ? "outbox_evento" : tabla.trim();
        coleccion = coleccion == null || coleccion.isBlank() ? "outbox_evento" : coleccion.trim();
        lote = lote == null || lote < 1 ? 50 : lote;
        intervalo = intervalo == null ? Duration.ofSeconds(2) : intervalo;
        arriendo = arriendo == null ? Duration.ofMinutes(1) : arriendo;
        esperaBase = esperaBase == null ? Duration.ofSeconds(2) : esperaBase;
        esperaMaxima = esperaMaxima == null ? Duration.ofMinutes(10) : esperaMaxima;
        maxIntentos = maxIntentos == null || maxIntentos < 1 ? 10 : maxIntentos;
    }

    /** Espera exponencial tras el intento numero {@code intentos} (1, 2, 3...), con tope. */
    public Duration esperaTras(int intentos) {
        int exponente = Math.min(Math.max(intentos - 1, 0), 20);
        Duration espera = esperaBase.multipliedBy(1L << exponente);
        return espera.compareTo(esperaMaxima) > 0 ? esperaMaxima : espera;
    }
}
