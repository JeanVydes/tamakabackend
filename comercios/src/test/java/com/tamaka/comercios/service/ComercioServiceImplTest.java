package com.tamaka.comercios.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.tamaka.common.event.EventPublisher;
import com.tamaka.comercios.domain.Comercio;
import com.tamaka.comercios.dto.request.ActualizarComercioRequest;
import com.tamaka.comercios.dto.request.CrearComercioRequest;
import com.tamaka.comercios.dto.request.CrearServicioRequest;
import com.tamaka.comercios.exception.RangoPersonasInvalidoException;
import com.tamaka.comercios.exception.RntDuplicadoException;
import com.tamaka.comercios.mapper.CatalogoMapper;
import com.tamaka.comercios.repository.ComercioRepository;
import com.tamaka.comercios.repository.ServicioRepository;

@ExtendWith(MockitoExtension.class)
class ComercioServiceImplTest {

    @Mock
    private ComercioRepository comercioRepository;

    @Mock
    private ServicioRepository servicioRepository;

    @Spy
    private CatalogoMapper catalogoMapper = new CatalogoMapper();

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private ComercioServiceImpl comercioService;

    @Test
    void rechazaRntDuplicadoSinGuardar() {
        when(comercioRepository.existsByRnt("RNT-1")).thenReturn(true);

        assertThatThrownBy(() -> comercioService.crear(7L,
                new CrearComercioRequest("Hotel Sol", "rnt-1", "HOSPEDAJE", null)))
                .isInstanceOf(RntDuplicadoException.class);

        verify(comercioRepository, never()).save(any());
    }

    @Test
    void operadorAjenoNoPuedeActualizarComercio() {
        Comercio ajeno = Comercio.crear(99L, "Hotel Sol", "RNT-2", "HOSPEDAJE", null);
        when(comercioRepository.findById(5L)).thenReturn(Optional.of(ajeno));

        assertThatThrownBy(() -> comercioService.actualizar(7L, 5L,
                new ActualizarComercioRequest("Nuevo", "HOSPEDAJE", null)))
                .isInstanceOf(AccessDeniedException.class);

        verify(eventPublisher, never()).publicar(any());
    }

    @Test
    void rechazaRangoDePersonasInvertido() {
        Comercio propio = Comercio.crear(7L, "Hotel Sol", "RNT-3", "HOSPEDAJE", null);
        when(comercioRepository.findById(5L)).thenReturn(Optional.of(propio));

        assertThatThrownBy(() -> comercioService.agregarServicio(7L, 5L,
                new CrearServicioRequest("Suite", new BigDecimal("150000"), 4, 2, null)))
                .isInstanceOf(RangoPersonasInvalidoException.class);

        verify(servicioRepository, never()).save(any());
    }
}
