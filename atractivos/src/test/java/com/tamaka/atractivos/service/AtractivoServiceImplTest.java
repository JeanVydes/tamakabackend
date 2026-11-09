package com.tamaka.atractivos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tamaka.atractivos.domain.Atractivo;
import com.tamaka.atractivos.domain.EstadoAtractivo;
import com.tamaka.atractivos.dto.request.CambiarEstadoAtractivoRequest;
import com.tamaka.atractivos.dto.request.EmitirAlertaRequest;
import com.tamaka.atractivos.mapper.AtractivoMapper;
import com.tamaka.atractivos.repository.AlertaSeguridadRepository;
import com.tamaka.atractivos.repository.AtractivoRepository;
import com.tamaka.common.error.ResourceNotFoundException;
import com.tamaka.common.event.EventPublisher;

@ExtendWith(MockitoExtension.class)
class AtractivoServiceImplTest {

    @Mock
    private AtractivoRepository atractivoRepository;

    @Mock
    private AlertaSeguridadRepository alertaRepository;

    @Spy
    private AtractivoMapper atractivoMapper = new AtractivoMapper();

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private AtractivoServiceImpl atractivoService;

    @Test
    void cambiarEstadoPublicaEventoConElNuevoEstado() {
        Atractivo atractivo = Atractivo.crear("Cascada", null, "NATURALEZA", 3L);
        when(atractivoRepository.findById(1L)).thenReturn(Optional.of(atractivo));

        var resultado = atractivoService.cambiarEstado(1L, new CambiarEstadoAtractivoRequest(EstadoAtractivo.CERRADO));

        assertThat(resultado.estado()).isEqualTo("CERRADO");
        verify(eventPublisher).publicar(any());
    }

    @Test
    void alertaSobreAtractivoInexistenteLanza404() {
        when(atractivoRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> atractivoService.emitirAlerta(3L, new EmitirAlertaRequest(9L, "Deslizamiento")))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
