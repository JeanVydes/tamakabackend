package com.tamaka.reservas.repository;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.tamaka.common.error.ResourceNotFoundException;
import com.tamaka.reservas.service.ServicioCatalogo;

/**
 * Adaptador de consulta puntual a Comercios (GET /api/servicios/{id}), la unica forma permitida de leer datos ajenos.
 * Pendiente: circuit breaker y reintentos.
 */
@Component
public class ServicioCatalogoHttpAdapter implements ServicioCatalogo {

    private final RestClient restClient;

    public ServicioCatalogoHttpAdapter(@Value("${tamaka.clientes.comercios.url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public ServicioRef obtener(Long servicioId) {
        try {
            ServicioRemoto remoto = restClient.get()
                    .uri("/api/servicios/{id}", servicioId)
                    .retrieve()
                    .body(ServicioRemoto.class);
            if (remoto == null) {
                throw new ResourceNotFoundException("Servicio", servicioId);
            }
            return new ServicioRef(remoto.id(), remoto.operadorId(),
                    remoto.minPersonas(), remoto.maxPersonas(), remoto.activo());
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException("Servicio", servicioId);
        }
    }

    /** Forma de la respuesta de Comercios (solo los campos necesarios). */
    private record ServicioRemoto(Long id, Long comercioId, Long operadorId, String nombre,
                                  BigDecimal precioBase, int minPersonas, int maxPersonas,
                                  boolean activo, Map<String, Object> atributos) {
    }
}
