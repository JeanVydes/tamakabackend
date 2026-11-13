package com.tamaka.common.event.http;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.tamaka.common.event.DespachadorEventos;
import com.tamaka.common.event.DomainEvent;

/**
 * Entrada HTTP interna del bus de eventos. No usa JWT: se autentica con el secreto compartido
 * (cabecera {@value HttpEventPublisher#CABECERA_SECRETO}), que verifica el despachador.
 * Cada SecurityConfig la deja publica a nivel de filtro; nunca debe exponerse por el API Gateway.
 * Respuestas: 202 procesado, 401 secreto invalido, 409 evento invalido o regla de negocio, 5xx reintentable.
 */
@RestController
public class ReceptorEventosController {

    private final DespachadorEventos despachador;

    public ReceptorEventosController(DespachadorEventos despachador) {
        this.despachador = despachador;
    }

    @PostMapping(HttpEventPublisher.RUTA_INTERNA)
    public ResponseEntity<Void> recibir(
            @RequestHeader(value = HttpEventPublisher.CABECERA_SECRETO, required = false) String secreto,
            @RequestBody DomainEvent evento) {
        despachador.recibir(secreto, evento);
        return ResponseEntity.accepted().build();
    }
}
