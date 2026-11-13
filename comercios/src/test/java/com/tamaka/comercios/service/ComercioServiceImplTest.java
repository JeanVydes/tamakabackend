package com.tamaka.comercios.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import com.tamaka.comercios.domain.Comercio;
import com.tamaka.comercios.domain.Servicio;
import com.tamaka.comercios.dto.request.ActualizarComercioRequest;
import com.tamaka.comercios.dto.request.CrearComercioRequest;
import com.tamaka.comercios.dto.request.CrearServicioRequest;
import com.tamaka.comercios.exception.RangoPersonasInvalidoException;
import com.tamaka.comercios.exception.RntDuplicadoException;
import com.tamaka.comercios.mapper.CatalogoMapper;
import com.tamaka.comercios.repository.ComercioRepository;
import com.tamaka.comercios.repository.ServicioRepository;
import com.tamaka.common.error.ResourceNotFoundException;
import com.tamaka.common.event.EventPublisher;

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

    @Test
    void cuentaSuspendidaRetiraComerciosYServiciosDelOperador() {
        Comercio comercio = Comercio.crear(7L, "Hotel Sol", "RNT-4", "HOSPEDAJE", null);
        ReflectionTestUtils.setField(comercio, "id", 3L);
        Servicio suite = Servicio.crear(comercio, "Suite", new BigDecimal("150000"), 1, 4, null);
        Servicio tour = Servicio.crear(comercio, "Tour", new BigDecimal("50000"), 1, 10, null);
        when(comercioRepository.findByOperadorId(7L)).thenReturn(List.of(comercio));
        when(servicioRepository.findActivosDeOperador(7L)).thenReturn(List.of(suite, tour));

        int desactivados = comercioService.desactivarPorCuentaSuspendida(7L);

        assertThat(desactivados).isEqualTo(2);
        assertThat(comercio.isActivo()).isFalse();
        assertThat(suite.isActivo()).isFalse();
        assertThat(tour.isActivo()).isFalse();
        verify(eventPublisher, times(2)).publicar(argThat(e -> "ServicioDesactivado".equals(e.tipo())
                && Long.valueOf(7L).equals(e.datoLong("operadorId"))));
    }

    @Test
    void detalleDeComercioInactivoEs404() {
        Comercio comercio = Comercio.crear(7L, "Hotel Sol", "RNT-5", "HOSPEDAJE", null);
        comercio.desactivar();
        when(comercioRepository.findById(3L)).thenReturn(Optional.of(comercio));

        assertThatThrownBy(() -> comercioService.obtener(3L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void agregarServicioPublicaServicioCreadoConLaCategoria() {
        Comercio propio = Comercio.crear(7L, "Hotel Sol", "RNT-6", "HOSPEDAJE", null);
        ReflectionTestUtils.setField(propio, "id", 3L);
        when(comercioRepository.findById(3L)).thenReturn(Optional.of(propio));
        when(servicioRepository.save(any(Servicio.class))).thenAnswer(inv -> {
            Servicio servicio = inv.getArgument(0);
            ReflectionTestUtils.setField(servicio, "id", 12L);
            return servicio;
        });

        comercioService.agregarServicio(7L, 3L, new CrearServicioRequest("Suite", new BigDecimal("150000"), 1, 4, null));

        verify(eventPublisher).publicar(argThat(e -> "ServicioCreado".equals(e.tipo())
                && "12".equals(e.agregadoId())
                && "HOSPEDAJE".equals(e.datoTexto("categoria"))));
    }
}
