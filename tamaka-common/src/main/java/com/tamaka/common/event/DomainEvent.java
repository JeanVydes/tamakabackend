package com.tamaka.common.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Evento de dominio publicado por un microservicio hacia el bus (HTTP en desarrollo, SNS/SQS en AWS).
 * El payload contiene solo identificadores y datos minimos (Ley 1581: minimizacion de datos personales).
 * Convencion de payload: ids como numeros, montos como texto decimal y fechas como texto ISO-8601,
 * para que el evento sobreviva la serializacion JSON sin perder precision.
 *
 * El {@code id} es unico por evento (UUID) y se conserva en reentregas: los consumidores sin idempotencia natural
 * lo usan para descartar duplicados.
 *
 * @param id         identificador unico del evento
 * @param tipo       nombre del evento, p. ej. "ReservaAceptada"
 * @param agregadoId identificador del agregado afectado
 * @param ocurridoEn instante de la ocurrencia
 * @param datos      datos minimos del evento (sin valores nulos)
 */
public record DomainEvent(
        String id,
        String tipo,
        String agregadoId,
        Instant ocurridoEn,
        Map<String, Object> datos
) {

    public DomainEvent {
        id = id == null || id.isBlank() ? UUID.randomUUID().toString() : id;
        Map<String, Object> copia = new LinkedHashMap<>();
        if (datos != null) {
            datos.forEach((clave, valor) -> {
                if (clave != null && valor != null) {
                    copia.put(clave, valor);
                }
            });
        }
        datos = Map.copyOf(copia);
        ocurridoEn = ocurridoEn == null ? Instant.now() : ocurridoEn;
    }

    /** Evento nuevo: se le asigna un id unico. */
    public DomainEvent(String tipo, String agregadoId, Instant ocurridoEn, Map<String, Object> datos) {
        this(null, tipo, agregadoId, ocurridoEn, datos);
    }

    public static DomainEvent de(String tipo, Object agregadoId, Map<String, Object> datos) {
        return new DomainEvent(tipo, String.valueOf(agregadoId), Instant.now(), datos);
    }

    /** Identificador del agregado como Long (los agregados de los servicios PostgreSQL son numericos). */
    public Long agregadoIdLong() {
        return aLong(agregadoId, "agregadoId");
    }

    /** Dato numerico entero; null si no viene en el payload. */
    public Long datoLong(String clave) {
        Object valor = datos.get(clave);
        return valor == null ? null : aLong(valor, clave);
    }

    /** Dato de texto; null si no viene en el payload. */
    public String datoTexto(String clave) {
        Object valor = datos.get(clave);
        return valor == null ? null : String.valueOf(valor);
    }

    /** Dato decimal (montos); acepta numero o texto. */
    public BigDecimal datoDecimal(String clave) {
        Object valor = datos.get(clave);
        if (valor == null) {
            return null;
        }
        if (valor instanceof BigDecimal decimal) {
            return decimal;
        }
        try {
            return new BigDecimal(String.valueOf(valor));
        } catch (NumberFormatException ex) {
            throw new EventoInvalidoException(tipo, clave);
        }
    }

    /** Dato instante; acepta Instant, texto ISO-8601 o epoch en milisegundos. */
    public Instant datoInstante(String clave) {
        Object valor = datos.get(clave);
        if (valor == null) {
            return null;
        }
        if (valor instanceof Instant instante) {
            return instante;
        }
        if (valor instanceof Number numero) {
            return Instant.ofEpochMilli(numero.longValue());
        }
        try {
            return Instant.parse(String.valueOf(valor));
        } catch (RuntimeException ex) {
            throw new EventoInvalidoException(tipo, clave);
        }
    }

    /** Dato lista de textos (p. ej. categorias). */
    public List<String> datoListaTexto(String clave) {
        Object valor = datos.get(clave);
        if (valor instanceof Collection<?> coleccion) {
            return coleccion.stream().filter(Objects::nonNull).map(String::valueOf).toList();
        }
        return List.of();
    }

    /** Dato obligatorio: falla con un error claro si el productor no lo envio. */
    public Long datoLongRequerido(String clave) {
        Long valor = datoLong(clave);
        if (valor == null) {
            throw new EventoInvalidoException(tipo, clave);
        }
        return valor;
    }

    public BigDecimal datoDecimalRequerido(String clave) {
        BigDecimal valor = datoDecimal(clave);
        if (valor == null) {
            throw new EventoInvalidoException(tipo, clave);
        }
        return valor;
    }

    public Instant datoInstanteRequerido(String clave) {
        Instant valor = datoInstante(clave);
        if (valor == null) {
            throw new EventoInvalidoException(tipo, clave);
        }
        return valor;
    }

    private Long aLong(Object valor, String clave) {
        if (valor instanceof Number numero) {
            return numero.longValue();
        }
        try {
            return Long.valueOf(String.valueOf(valor).trim());
        } catch (NumberFormatException ex) {
            throw new EventoInvalidoException(tipo, clave);
        }
    }
}
