package com.tamaka.reservas.scheduler;

import java.time.LocalDate;
import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.tamaka.reservas.service.ReservaService;

/**
 * Disparador periodico de CONFIRMADA -> FINALIZADA cuando la fecha del servicio ya paso.
 * No contiene reglas: delega en el servicio. La fecha se evalua en la zona horaria de la operacion (Santa Marta).
 */
@Component
public class FinalizacionReservasJob {

    private static final Logger log = LoggerFactory.getLogger(FinalizacionReservasJob.class);

    private final ReservaService reservaService;
    private final ZoneId zona;

    public FinalizacionReservasJob(ReservaService reservaService,
                                   @Value("${tamaka.reservas.zona-horaria:America/Bogota}") String zona) {
        this.reservaService = reservaService;
        this.zona = ZoneId.of(zona);
    }

    @Scheduled(fixedDelayString = "${tamaka.reservas.intervalo-finalizacion:PT1H}")
    public void finalizarConcluidas() {
        int finalizadas = reservaService.finalizarConcluidas(LocalDate.now(zona));
        if (finalizadas > 0) {
            log.info("reservas finalizadas={}", finalizadas);
        }
    }
}
