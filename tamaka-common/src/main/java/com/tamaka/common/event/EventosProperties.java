package com.tamaka.common.event;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracion del bus de eventos ({@code tamaka.eventos.*}).
 *
 * @param transporte   "http" (por defecto: POST a los suscriptores configurados) o "log" (solo registra)
 * @param secreto      secreto compartido que viaja en la cabecera {@code X-Tamaka-Eventos-Secreto}.
 *                     Vacio: el servicio no acepta eventos entrantes y no publica por HTTP (fail closed)
 * @param suscriptores tipo de evento -> URLs de los endpoints {@code /internal/eventos} de los consumidores
 * @param timeout      tiempo maximo de cada entrega HTTP
 * @param reintentos   reintentos ante errores de red o 5xx (los 4xx no se reintentan)
 */
@ConfigurationProperties(prefix = "tamaka.eventos")
public record EventosProperties(
        String transporte,
        String secreto,
        Map<String, List<String>> suscriptores,
        Duration timeout,
        Integer reintentos
) {

    public EventosProperties {
        transporte = transporte == null || transporte.isBlank() ? "http" : transporte.trim().toLowerCase(Locale.ROOT);
        secreto = secreto == null ? "" : secreto;
        // Busqueda sin distinguir mayusculas: las variables de entorno llegan en minusculas.
        TreeMap<String, List<String>> normalizados = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (suscriptores != null) {
            suscriptores.forEach((tipo, urls) -> normalizados.put(tipo, urls == null
                    ? List.of()
                    : urls.stream().map(String::trim).filter(url -> !url.isEmpty()).toList()));
        }
        suscriptores = normalizados;
        timeout = timeout == null ? Duration.ofSeconds(5) : timeout;
        reintentos = reintentos == null || reintentos < 0 ? 3 : reintentos;
    }

    public List<String> suscriptoresDe(String tipo) {
        return suscriptores.getOrDefault(tipo, List.of());
    }

    public boolean tieneSecreto() {
        return !secreto.isBlank();
    }
}
