package com.tamaka.notificaciones.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tamaka.common.error.ResourceNotFoundException;
import com.tamaka.notificaciones.exception.TipoNotificacionDesconocidoException;
import com.tamaka.notificaciones.repository.NotificacionRepository;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceImplTest {

    @Mock
    private NotificacionRepository notificacionRepository;

    @InjectMocks
    private NotificacionServiceImpl notificacionService;

    @Test
    void rechazaEventoSinPlantilla() {
        assertThatThrownBy(() -> notificacionService.notificar(1L, "EventoInventado", "es"))
                .isInstanceOf(TipoNotificacionDesconocidoException.class);

        verify(notificacionRepository, never()).save(any());
    }

    @Test
    void noRevelaNotificacionAjena() {
        when(notificacionRepository.findByIdAndDestinatarioId("n1", 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificacionService.marcarLeida(2L, "n1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
