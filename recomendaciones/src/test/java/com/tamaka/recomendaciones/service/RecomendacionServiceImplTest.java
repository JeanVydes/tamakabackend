package com.tamaka.recomendaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tamaka.recomendaciones.repository.RecomendacionRedisRepository;

@ExtendWith(MockitoExtension.class)
class RecomendacionServiceImplTest {

    @Mock
    private RecomendacionRedisRepository repository;

    private RecomendacionServiceImpl recomendacionService;

    @BeforeEach
    void crearServicio() {
        recomendacionService = new RecomendacionServiceImpl(repository, 10);
    }

    @Test
    void priorizaItemsDeLasCategoriasPreferidas() {
        when(repository.categoriasDe(1L)).thenReturn(Set.of("NATURALEZA"));
        when(repository.idsPorCategorias("ATRACTIVO", Set.of("NATURALEZA"))).thenReturn(Set.of("4", "9"));

        List<Long> resultado = recomendacionService.recomendar("ATRACTIVO", 1L);

        // Set.of no garantiza orden de iteracion: se verifica el contenido, no el orden.
        assertThat(resultado).containsExactlyInAnyOrder(4L, 9L);
    }

    @Test
    void sinPreferenciasUsaLosMasRecientes() {
        when(repository.categoriasDe(2L)).thenReturn(Set.of());
        when(repository.masRecientes("EVENTO", 10)).thenReturn(List.of("7", "3"));

        assertThat(recomendacionService.recomendar("EVENTO", 2L)).containsExactly(7L, 3L);
    }

    @Test
    void atractivoCerradoDejaDeRecomendarse() {
        recomendacionService.registrarAtractivo(4L, "PLAYA", "CERRADO");

        verify(repository).retirarItem("ATRACTIVO", 4L);
        verify(repository, never()).registrarItem(any(), any(), any(), anyDouble());
    }

    @Test
    void eventoSinCategoriaQuedaEnGeneral() {
        recomendacionService.registrarItem("EVENTO", 8L, null);

        verify(repository).registrarItem(eq("EVENTO"), eq(8L), eq("GENERAL"), anyDouble());
    }
}
