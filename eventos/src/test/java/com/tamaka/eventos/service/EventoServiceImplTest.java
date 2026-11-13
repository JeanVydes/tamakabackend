package com.tamaka.eventos.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tamaka.common.event.EventPublisher;
import com.tamaka.eventos.dto.request.PublicarEventoRequest;
import com.tamaka.eventos.exception.RangoFechasInvalidoException;
import com.tamaka.eventos.mapper.EventoMapper;
import com.tamaka.eventos.repository.EventoRepository;

@ExtendWith(MockitoExtension.class)
class EventoServiceImplTest {

    @Mock
    private EventoRepository eventoRepository;

    @Spy
    private EventoMapper eventoMapper = new EventoMapper();

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private EventoServiceImpl eventoService;

    @Test
    void rechazaEventoCuyaFechaFinEsAnteriorAlInicio() {
        var request = new PublicarEventoRequest("Festival", null, "Plaza", "CULTURA",
                LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 1));

        assertThatThrownBy(() -> eventoService.publicar(5L, request))
                .isInstanceOf(RangoFechasInvalidoException.class);

        verify(eventoRepository, never()).save(any());
    }
}
