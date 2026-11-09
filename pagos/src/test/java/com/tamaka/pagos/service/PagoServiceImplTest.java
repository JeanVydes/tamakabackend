package com.tamaka.pagos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

import com.tamaka.common.event.EventPublisher;
import com.tamaka.pagos.domain.EstadoPago;
import com.tamaka.pagos.domain.Pago;
import com.tamaka.pagos.dto.request.WebhookPasarelaRequest;
import com.tamaka.pagos.mapper.PagoMapper;
import com.tamaka.pagos.repository.PagoRepository;
import com.tamaka.pagos.repository.ReservaPagableRepository;

@ExtendWith(MockitoExtension.class)
class PagoServiceImplTest {

    private static final String SECRETO = "secreto-de-prueba-con-longitud-suficiente";
    private static final Instant AHORA = Instant.parse("2026-11-09T10:00:00Z");

    @Mock
    private PagoRepository pagoRepository;

    @Mock
    private ReservaPagableRepository reservaPagableRepository;

    @Mock
    private PasarelaPagos pasarela;

    @Spy
    private PagoMapper pagoMapper = new PagoMapper();

    @Mock
    private EventPublisher eventPublisher;

    @Spy
    private PagosProperties properties = new PagosProperties(SECRETO);

    @InjectMocks
    private PagoServiceImpl pagoService;

    @Test
    void rechazaWebhookConFirmaInvalidaSinTocarElPago() {
        var peticion = new WebhookPasarelaRequest("TMK-1", "APROBADO", new BigDecimal("80000"));

        assertThatThrownBy(() -> pagoService.procesarWebhook("firma-falsa", peticion))
                .isInstanceOf(BadCredentialsException.class);

        verify(pagoRepository, never()).findByReferenciaPasarela(any());
        verify(eventPublisher, never()).publicar(any());
    }

    @Test
    void webhookAprobadoPublicaPagoAprobadoUnaVez() {
        Pago pago = Pago.iniciar(5L, 1L, new BigDecimal("80000"), "TMK-1", AHORA);
        var peticion = new WebhookPasarelaRequest("TMK-1", "APROBADO", new BigDecimal("80000"));
        when(pagoRepository.findByReferenciaPasarela("TMK-1")).thenReturn(Optional.of(pago));
        when(pagoRepository.saveAndFlush(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));

        pagoService.procesarWebhook(FirmaWebhook.firmar(SECRETO, peticion), peticion);

        assertThat(pago.getEstado()).isEqualTo(EstadoPago.APROBADO);
        verify(eventPublisher).publicar(any());
    }

    @Test
    void webhookRepetidoNoVuelvePublicarNada() {
        Pago pago = Pago.iniciar(5L, 1L, new BigDecimal("80000"), "TMK-1", AHORA);
        pago.aprobar(AHORA);
        var peticion = new WebhookPasarelaRequest("TMK-1", "APROBADO", new BigDecimal("80000"));
        when(pagoRepository.findByReferenciaPasarela("TMK-1")).thenReturn(Optional.of(pago));

        pagoService.procesarWebhook(FirmaWebhook.firmar(SECRETO, peticion), peticion);

        verify(eventPublisher, never()).publicar(any());
        verify(pagoRepository, never()).saveAndFlush(any());
    }
}
