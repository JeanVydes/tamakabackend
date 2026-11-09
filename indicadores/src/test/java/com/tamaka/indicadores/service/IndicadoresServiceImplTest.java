package com.tamaka.indicadores.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}
