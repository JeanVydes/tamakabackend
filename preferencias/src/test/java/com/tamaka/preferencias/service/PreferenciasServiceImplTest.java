package com.tamaka.preferencias.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.tamaka.common.event.EventPublisher;
import com.tamaka.preferencias.domain.Favorito;
import com.tamaka.preferencias.dto.request.GuardarFavoritoRequest;
import com.tamaka.preferencias.exception.FavoritoDuplicadoException;
import com.tamaka.preferencias.mapper.PreferenciasMapper;
import com.tamaka.preferencias.repository.FavoritoRepository;
import com.tamaka.preferencias.repository.PreferenciaRepository;

@ExtendWith(MockitoExtension.class)
class PreferenciasServiceImplTest {

    @Mock
    private PreferenciaRepository preferenciaRepository;

    @Mock
    private FavoritoRepository favoritoRepository;

    @Spy
    private PreferenciasMapper mapper = new PreferenciasMapper();

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private PreferenciasServiceImpl preferenciasService;

    @Test
    void rechazaFavoritoDuplicado() {
        when(favoritoRepository.existsByTuristaIdAndTipoAndRefId(1L, "ATRACTIVO", "7")).thenReturn(true);

        assertThatThrownBy(() -> preferenciasService.guardarFavorito(1L,
                new GuardarFavoritoRequest("ATRACTIVO", "7")))
                .isInstanceOf(FavoritoDuplicadoException.class);

        verify(favoritoRepository, never()).save(any());
    }

    @Test
    void noPermiteEliminarFavoritoAjeno() {
        Favorito ajeno = new Favorito(99L, "EVENTO", "3");
        when(favoritoRepository.findById("abc")).thenReturn(Optional.of(ajeno));

        assertThatThrownBy(() -> preferenciasService.eliminarFavorito(1L, "abc"))
                .isInstanceOf(AccessDeniedException.class);

        verify(favoritoRepository, never()).delete(any());
    }

    @Test
    void guardaFavoritoDeComercioYPublicaConTurista() {
        when(favoritoRepository.existsByTuristaIdAndTipoAndRefId(1L, "COMERCIO", "3")).thenReturn(false);
        when(favoritoRepository.save(any(Favorito.class))).thenAnswer(inv -> inv.getArgument(0));

        var guardado = preferenciasService.guardarFavorito(1L, new GuardarFavoritoRequest("COMERCIO", "3"));

        assertThat(guardado.tipo()).isEqualTo("COMERCIO");
        verify(eventPublisher).publicar(argThat(e -> "FavoritoGuardado".equals(e.tipo())
                && Long.valueOf(1L).equals(e.datoLong("turistaId"))));
    }

    @Test
    void eliminarFavoritoPublicaFavoritoEliminado() {
        when(favoritoRepository.findById("f1")).thenReturn(Optional.of(new Favorito(1L, "ATRACTIVO", "4")));

        preferenciasService.eliminarFavorito(1L, "f1");

        verify(eventPublisher).publicar(argThat(e -> "FavoritoEliminado".equals(e.tipo())
                && "4".equals(e.datoTexto("refId"))));
    }
}
