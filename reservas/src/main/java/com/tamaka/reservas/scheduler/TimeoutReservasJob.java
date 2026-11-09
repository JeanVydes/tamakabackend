package com.tamaka.reservas.scheduler;

import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tamaka.reservas.service.ReservaService;

/**
 * Disparador periodico de los timeouts (RF-RES-14 y RF-RES-15). No contiene reglas: delega en el servicio.
 * Es idempotente: cada solicitud se cancela con control de version (@Version); varias replicas no duplican cancelaciones.
 */
@Component
public class TimeoutReservasJob {

    private static final Logger log = LoggerFactory.getLogger(TimeoutReservasJob.class);

    private final ReservaService reservaService;

    public TimeoutReservasJob(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @Scheduled(fixedDelayString = "${tamaka.reservas.intervalo-timeouts:PT1M}")
    public void cancelarVencidas() {
        int canceladas = reservaService.cancelarVencidas(Instant.now());
        if (canceladas > 0) {
            log.info("timeouts aplicados canceladas={}", canceladas);
        }
    }
}
