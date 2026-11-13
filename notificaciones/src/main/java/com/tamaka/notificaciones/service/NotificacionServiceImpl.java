package com.tamaka.notificaciones.service;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.tamaka.common.error.ResourceNotFoundException;
import com.tamaka.notificaciones.domain.EventoProcesado;
import com.tamaka.notificaciones.domain.IdiomaUsuario;
import com.tamaka.notificaciones.domain.Notificacion;
import com.tamaka.notificaciones.domain.Seguimiento;
import com.tamaka.notificaciones.dto.response.NotificacionResponse;
import com.tamaka.notificaciones.exception.TipoNotificacionDesconocidoException;
import com.tamaka.notificaciones.repository.EventoProcesadoRepository;
import com.tamaka.notificaciones.repository.IdiomaUsuarioRepository;
import com.tamaka.notificaciones.repository.NotificacionRepository;
import com.tamaka.notificaciones.repository.SeguimientoRepository;

/**
 * Pendiente: canal de correo y push (SES / FCM). Hoy la notificacion queda persistida y registrada en el log.
 * Sin transacciones: MongoDB no requiere @Transactional para documentos individuales.
 */
@Service
public class NotificacionServiceImpl implements NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionServiceImpl.class);
    private static final String TIPO_ATRACTIVO = "ATRACTIVO";

    /** A quien se dirige cada aviso de un evento de reservas o pagos. */
    private enum Rol { TURISTA, OPERADOR }

    private record Destino(Rol rol, String plantilla) {
    }

    /** Regla de destinatarios por evento (RF-NOT-01, 06, 11 y 12). */
    private static final Map<String, List<Destino>> DESTINOS = Map.of(
            "ReservaSolicitada", List.of(
                    new Destino(Rol.TURISTA, "ReservaSolicitadaTurista"),
                    new Destino(Rol.OPERADOR, "ReservaSolicitada")),
            "ReservaAceptada", List.of(new Destino(Rol.TURISTA, "ReservaAceptada")),
            "ReservaRechazada", List.of(new Destino(Rol.TURISTA, "ReservaRechazada")),
            "ReservaCancelada", List.of(
                    new Destino(Rol.TURISTA, "ReservaCancelada"),
                    new Destino(Rol.OPERADOR, "ReservaCancelada")),
            "PagoAprobado", List.of(
                    new Destino(Rol.TURISTA, "PagoAprobado"),
                    new Destino(Rol.OPERADOR, "PagoAprobadoOperador")),
            "PagoRechazado", List.of(new Destino(Rol.TURISTA, "PagoRechazado")));

    private final NotificacionRepository notificacionRepository;
    private final IdiomaUsuarioRepository idiomaRepository;
    private final SeguimientoRepository seguimientoRepository;
    private final EventoProcesadoRepository eventoProcesadoRepository;

    public NotificacionServiceImpl(NotificacionRepository notificacionRepository,
                                   IdiomaUsuarioRepository idiomaRepository,
                                   SeguimientoRepository seguimientoRepository,
                                   EventoProcesadoRepository eventoProcesadoRepository) {
        this.notificacionRepository = notificacionRepository;
        this.idiomaRepository = idiomaRepository;
        this.seguimientoRepository = seguimientoRepository;
        this.eventoProcesadoRepository = eventoProcesadoRepository;
    }

    @Override
    public void notificar(Long destinatarioId, String tipoEvento, String idioma) {
        PlantillasNotificacion.Texto texto = PlantillasNotificacion.buscar(tipoEvento, idioma)
                .orElseThrow(() -> new TipoNotificacionDesconocidoException(tipoEvento));
        Notificacion notificacion = new Notificacion(destinatarioId, tipoEvento,
                "en".equals(idioma) ? "en" : "es", texto.titulo(), texto.cuerpo());
        notificacionRepository.save(notificacion);
        log.info("notificacion creada tipo={}", tipoEvento);
    }

    @Override
    public int notificarEvento(String eventoId, String tipoEvento, Long turistaId, Long operadorId) {
        List<Destino> destinos = DESTINOS.get(tipoEvento);
        if (destinos == null) {
            throw new TipoNotificacionDesconocidoException(tipoEvento);
        }
        if (!reclamarEvento(eventoId, tipoEvento)) {
            return 0;
        }
        int creadas = 0;
        for (Destino destino : destinos) {
            Long destinatario = destino.rol() == Rol.TURISTA ? turistaId : operadorId;
            if (destinatario != null) {
                notificar(destinatario, destino.plantilla(), idiomaDe(destinatario));
                creadas++;
            }
        }
        return creadas;
    }

    @Override
    public int notificarSeguidoresDeAtractivo(String eventoId, String tipoEvento, Long atractivoId) {
        if (!reclamarEvento(eventoId, tipoEvento)) {
            return 0;
        }
        List<Seguimiento> seguidores = seguimientoRepository.findByTipoAndRefId(TIPO_ATRACTIVO,
                String.valueOf(atractivoId));
        for (Seguimiento seguimiento : seguidores) {
            notificar(seguimiento.getTuristaId(), tipoEvento, idiomaDe(seguimiento.getTuristaId()));
        }
        return seguidores.size();
    }

    @Override
    public void registrarIdioma(Long usuarioId, String idioma) {
        IdiomaUsuario preferencia = idiomaRepository.findById(usuarioId)
                .orElseGet(() -> new IdiomaUsuario(usuarioId, idioma));
        preferencia.cambiar(idioma);
        idiomaRepository.save(preferencia);
    }

    @Override
    public void registrarSeguimiento(Long turistaId, String tipo, String refId) {
        if (!seguimientoRepository.existsByTuristaIdAndTipoAndRefId(turistaId, tipo, refId)) {
            seguimientoRepository.save(new Seguimiento(turistaId, tipo, refId));
        }
    }

    @Override
    public void eliminarSeguimiento(Long turistaId, String tipo, String refId) {
        seguimientoRepository.deleteByTuristaIdAndTipoAndRefId(turistaId, tipo, refId);
    }

    @Override
    public List<NotificacionResponse> misNotificaciones(Long usuarioId) {
        return notificacionRepository.findByDestinatarioIdOrderByCreadaEnDesc(usuarioId).stream()
                .map(n -> new NotificacionResponse(n.getId(), n.getTipo(), n.getTitulo(), n.getCuerpo(),
                        n.isLeida(), n.getCreadaEn()))
                .toList();
    }

    @Override
    public void marcarLeida(Long usuarioId, String notificacionId) {
        // Mismo error si no existe o es ajena: no se revela si el id pertenece a otro usuario.
        Notificacion notificacion = notificacionRepository.findByIdAndDestinatarioId(notificacionId, usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Notificacion", notificacionId));
        notificacion.marcarLeida();
        notificacionRepository.save(notificacion);
    }

    /**
     * Registra el id del evento antes de crear avisos. El insert falla con clave duplicada si el evento ya se
     * proceso (tambien entre replicas concurrentes), y entonces se descarta. Si el proceso cae justo despues de la
     * marca se pierden esos avisos: se prefiere "como mucho una vez" a avisos duplicados.
     */
    private boolean reclamarEvento(String eventoId, String tipoEvento) {
        try {
            eventoProcesadoRepository.insert(new EventoProcesado(eventoId, tipoEvento));
            return true;
        } catch (DuplicateKeyException ex) {
            log.info("evento duplicado descartado tipo={} id={}", tipoEvento, eventoId);
            return false;
        }
    }

    private String idiomaDe(Long usuarioId) {
        return idiomaRepository.findById(usuarioId)
                .map(IdiomaUsuario::getIdioma)
                .orElse(IdiomaUsuario.IDIOMA_POR_DEFECTO);
    }
}
