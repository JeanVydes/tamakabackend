package com.tamaka.notificaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import com.tamaka.common.error.ResourceNotFoundException;
import com.tamaka.notificaciones.domain.EventoProcesado;
import com.tamaka.notificaciones.domain.IdiomaUsuario;
import com.tamaka.notificaciones.domain.Notificacion;
import com.tamaka.notificaciones.domain.Seguimiento;
import com.tamaka.notificaciones.exception.TipoNotificacionDesconocidoException;
import com.tamaka.notificaciones.repository.EventoProcesadoRepository;
import com.tamaka.notificaciones.repository.IdiomaUsuarioRepository;
import com.tamaka.notificaciones.repository.NotificacionRepository;
import com.tamaka.notificaciones.repository.SeguimientoRepository;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceImplTest {

    @Mock
    private NotificacionRepository notificacionRepository;

    @Mock
    private IdiomaUsuarioRepository idiomaRepository;

    @Mock
    private SeguimientoRepository seguimientoRepository;

    @Mock
    private EventoProcesadoRepository eventoProcesadoRepository;

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

    @Test
    void reservaSolicitadaAvisaAlTuristaYAlOperadorEnSuIdioma() {
        when(idiomaRepository.findById(1L)).thenReturn(Optional.of(new IdiomaUsuario(1L, "en")));
        when(idiomaRepository.findById(7L)).thenReturn(Optional.empty());

        int creadas = notificacionService.notificarEvento("ev-1", "ReservaSolicitada", 1L, 7L);

        assertThat(creadas).isEqualTo(2);
        ArgumentCaptor<Notificacion> guardadas = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository, times(2)).save(guardadas.capture());
        assertThat(guardadas.getAllValues())
                .extracting(Notificacion::getDestinatarioId, Notificacion::getTipo, Notificacion::getIdioma)
                .containsExactly(
                        tuple(1L, "ReservaSolicitadaTurista", "en"),
                        tuple(7L, "ReservaSolicitada", "es"));
    }

    @Test
    void reservaRechazadaSoloAvisaAlTurista() {
        when(idiomaRepository.findById(1L)).thenReturn(Optional.empty());

        assertThat(notificacionService.notificarEvento("ev-2", "ReservaRechazada", 1L, 7L)).isEqualTo(1);
        verify(notificacionRepository).save(argThat(n -> n.getDestinatarioId().equals(1L)));
    }

    @Test
    void alertaDeSeguridadAvisaASeguidoresDelAtractivo() {
        when(seguimientoRepository.findByTipoAndRefId("ATRACTIVO", "4"))
                .thenReturn(List.of(new Seguimiento(1L, "ATRACTIVO", "4"), new Seguimiento(2L, "ATRACTIVO", "4")));
        when(idiomaRepository.findById(any())).thenReturn(Optional.empty());

        assertThat(notificacionService.notificarSeguidoresDeAtractivo("ev-3", "AlertaSeguridadEmitida", 4L)).isEqualTo(2);
        verify(notificacionRepository, times(2)).save(any());
    }

    @Test
    void favoritoRepetidoNoDuplicaElSeguimiento() {
        when(seguimientoRepository.existsByTuristaIdAndTipoAndRefId(1L, "ATRACTIVO", "4")).thenReturn(true);

        notificacionService.registrarSeguimiento(1L, "ATRACTIVO", "4");

        verify(seguimientoRepository, never()).save(any());
    }

    @Test
    void eventoRepetidoNoCreaAvisosDuplicados() {
        when(eventoProcesadoRepository.insert(any(EventoProcesado.class)))
                .thenThrow(new DuplicateKeyException("ev-1 ya procesado"));

        int creadas = notificacionService.notificarEvento("ev-1", "PagoAprobado", 1L, 7L);

        assertThat(creadas).isZero();
        verify(notificacionRepository, never()).save(any());
    }

    @Test
    void alertaRepetidaNoVuelveAAvisarASeguidores() {
        when(eventoProcesadoRepository.insert(any(EventoProcesado.class)))
                .thenThrow(new DuplicateKeyException("ev-3 ya procesado"));

        assertThat(notificacionService.notificarSeguidoresDeAtractivo("ev-3", "AlertaSeguridadEmitida", 4L)).isZero();
        verify(seguimientoRepository, never()).findByTipoAndRefId(any(), any());
    }
}
