package com.tamaka.indicadores.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tamaka.indicadores.domain.RegistroDemanda;
import com.tamaka.indicadores.dto.response.ConteoResponse;
import com.tamaka.indicadores.exception.RangoFechasInvalidoException;
import com.tamaka.indicadores.repository.RegistroDemandaRepository;

@Service
@Transactional(readOnly = true)
public class IndicadoresServiceImpl implements IndicadoresService {

    private final RegistroDemandaRepository repository;

    public IndicadoresServiceImpl(RegistroDemandaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ConteoResponse> demanda(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        return repository.contarPorTipo(desde, hasta).stream()
                .map(c -> new ConteoResponse(c.getTipo(), c.getTotal()))
                .toList();
    }

    @Override
    public List<ConteoResponse> indicadoresDelOperador(Long operadorId, LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);
        return repository.contarPorTipoDeOperador(operadorId, desde, hasta).stream()
                .map(c -> new ConteoResponse(c.getTipo(), c.getTotal()))
                .toList();
    }

    @Override
    @Transactional
    public void registrarHecho(String tipo, Long operadorId, Long referenciaId, Instant ocurridoEn) {
        repository.save(new RegistroDemanda(tipo, operadorId, referenciaId, ocurridoEn));
    }

    private static void validarRango(LocalDate desde, LocalDate hasta) {
        if (hasta.isBefore(desde)) {
            throw new RangoFechasInvalidoException(desde, hasta);
        }
    }
}
