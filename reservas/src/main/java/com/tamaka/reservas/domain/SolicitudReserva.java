package com.tamaka.reservas.domain;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

import com.tamaka.reservas.exception.SolicitudYaRespondidaException;
import com.tamaka.reservas.exception.TransicionReservaInvalidaException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Agregado Solicitud de reserva. Las transiciones se validan aqui; el servicio solo orquesta.
 * El @Version evita que dos replicas del job de timeouts cancelen la misma solicitud dos veces.
 */
@Entity
@Table(name = "solicitud_reserva")
public class SolicitudReserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(name = "turista_id", nullable = false)
    private Long turistaId;

    @Column(name = "servicio_id", nullable = false)
    private Long servicioId;

    @Column(name = "operador_id", nullable = false)
    private Long operadorId;

    @Column(name = "fecha_reserva", nullable = false)
    private LocalDate fechaReserva;

    @Column(name = "numero_personas", nullable = false)
    private int numeroPersonas;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoReserva estado;

    @Column(name = "valor_a_pagar", precision = 12, scale = 2)
    private BigDecimal valorAPagar;

    @Column(name = "observacion_operador", length = 500)
    private String observacionOperador;

    @Column(name = "creada_en", nullable = false)
    private Instant creadaEn;

    @Column(name = "respondida_en")
    private Instant respondidaEn;

    @Column(name = "plazo_pago")
    private Instant plazoPago;

    protected SolicitudReserva() {
    }

    private SolicitudReserva(Long turistaId, Long servicioId, Long operadorId,
                             LocalDate fechaReserva, int numeroPersonas, Instant ahora) {
        this.turistaId = turistaId;
        this.servicioId = servicioId;
        this.operadorId = operadorId;
        this.fechaReserva = fechaReserva;
        this.numeroPersonas = numeroPersonas;
        this.estado = EstadoReserva.PENDIENTE;
        this.creadaEn = ahora;
    }

    public static SolicitudReserva crear(Long turistaId, Long servicioId, Long operadorId,
                                         LocalDate fechaReserva, int numeroPersonas, Instant ahora) {
        return new SolicitudReserva(turistaId, servicioId, operadorId, fechaReserva, numeroPersonas, ahora);
    }

    /** PENDIENTE -> ACEPTADA. Fija el valor a pagar y el plazo de pago. */
    public void aceptar(BigDecimal valorAPagar, String observacion, Instant ahora, Duration plazoPago) {
        exigirPendiente();
        this.estado = EstadoReserva.ACEPTADA;
        this.valorAPagar = valorAPagar;
        this.observacionOperador = observacion;
        this.respondidaEn = ahora;
        this.plazoPago = ahora.plus(plazoPago);
    }

    /** PENDIENTE -> RECHAZADA. */
    public void rechazar(String observacion, Instant ahora) {
        exigirPendiente();
        this.estado = EstadoReserva.RECHAZADA;
        this.observacionOperador = observacion;
        this.respondidaEn = ahora;
    }

    /**
     * ACEPTADA -> CONFIRMADA cuando llega el pago. Condicional: si la solicitud ya no esta ACEPTADA
     * (p. ej., cancelada por timeout) no cambia y devuelve false.
     */
    public boolean confirmarPorPago() {
        if (estado != EstadoReserva.ACEPTADA) {
            return false;
        }
        this.estado = EstadoReserva.CONFIRMADA;
        return true;
    }

    /** Cancela desde PENDIENTE, ACEPTADA o CONFIRMADA. */
    public void cancelar() {
        if (estado != EstadoReserva.PENDIENTE && estado != EstadoReserva.ACEPTADA
                && estado != EstadoReserva.CONFIRMADA) {
            throw new TransicionReservaInvalidaException(id, estado, EstadoReserva.CANCELADA);
        }
        this.estado = EstadoReserva.CANCELADA;
    }

    /** CONFIRMADA -> FINALIZADA, cuando la fecha del servicio ya paso. */
    public void finalizar() {
        if (estado != EstadoReserva.CONFIRMADA) {
            throw new TransicionReservaInvalidaException(id, estado, EstadoReserva.FINALIZADA);
        }
        this.estado = EstadoReserva.FINALIZADA;
    }

    public boolean respuestaVencida(Instant ahora, Duration plazoRespuesta) {
        return estado == EstadoReserva.PENDIENTE && creadaEn.plus(plazoRespuesta).isBefore(ahora);
    }

    public boolean pagoVencido(Instant ahora) {
        return estado == EstadoReserva.ACEPTADA && plazoPago != null && plazoPago.isBefore(ahora);
    }

    public boolean esDeTurista(Long turistaId) {
        return this.turistaId.equals(turistaId);
    }

    public boolean esDeOperador(Long operadorId) {
        return this.operadorId.equals(operadorId);
    }

    private void exigirPendiente() {
        if (estado != EstadoReserva.PENDIENTE) {
            throw new SolicitudYaRespondidaException(id);
        }
    }

    public Long getId() {
        return id;
    }

    public Long getTuristaId() {
        return turistaId;
    }

    public Long getServicioId() {
        return servicioId;
    }

    public Long getOperadorId() {
        return operadorId;
    }

    public LocalDate getFechaReserva() {
        return fechaReserva;
    }

    public int getNumeroPersonas() {
        return numeroPersonas;
    }

    public EstadoReserva getEstado() {
        return estado;
    }

    public BigDecimal getValorAPagar() {
        return valorAPagar;
    }

    public String getObservacionOperador() {
        return observacionOperador;
    }

    public Instant getCreadaEn() {
        return creadaEn;
    }

    public Instant getRespondidaEn() {
        return respondidaEn;
    }

    public Instant getPlazoPago() {
        return plazoPago;
    }

    @Override
    public boolean equals(Object otro) {
        if (this == otro) {
            return true;
        }
        return otro instanceof SolicitudReserva that && id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
