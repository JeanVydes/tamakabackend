package com.tamaka.indicadores.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tamaka.indicadores.domain.RegistroDemanda;
import com.tamaka.indicadores.exception.RangoFechasInvalidoException;
import com.tamaka.indicadores.repository.RegistroDemandaRepository;

@ExtendWith(MockitoExtension.class)
class IndicadoresServiceImplTest {

    @Mock
    private RegistroDemandaRepository repository;

    @InjectMocks
    private IndicadoresServiceImpl indicadoresService;

    @Test
    void rechazaRangoConHastaAnteriorADesde() {
        assertThatThrownBy(() -> indicadoresService.demanda(
                LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 1)))
                .isInstanceOf(RangoFechasInvalidoException.class);
    }

    @Test
    void hechoRepetidoNoSeRegistraDosVeces() {
        when(repository.existsByTipoAndReferenciaId("PAGO_APROBADO", 5L)).thenReturn(true);

        boolean registrado = indicadoresService.registrarHecho("PAGO_APROBADO", 7L, 5L, Instant.now());

        assertThat(registrado).isFalse();
        verify(repository, never()).save(any());
    }

    @Test
    void hechoNuevoSeRegistra() {
        when(repository.existsByTipoAndReferenciaId("RESERVA_SOLICITADA", 5L)).thenReturn(false);

        assertThat(indicadoresService.registrarHecho("RESERVA_SOLICITADA", 7L, 5L, Instant.now())).isTrue();
        verify(repository).save(any(RegistroDemanda.class));
    }
}
