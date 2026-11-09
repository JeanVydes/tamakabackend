package com.tamaka.notificaciones.service;

import java.util.Map;
import java.util.Optional;

/**
 * Plantillas bilingues (es/en) por tipo de evento (SRS RNF Multi-idioma; arquitectura sec. 5).
 * Las variables se sustituyen con {nombre}; no se incluyen datos personales en el texto.
 */
final class PlantillasNotificacion {

    /** Par titulo/cuerpo para un idioma. */
    record Texto(String titulo, String cuerpo) {
    }

    private static final Map<String, Map<String, Texto>> PLANTILLAS = Map.of(
            "ReservaSolicitada", Map.of(
                    "es", new Texto("Nueva solicitud de reserva", "Tienes una nueva solicitud para revisar."),
                    "en", new Texto("New booking request", "You have a new booking request to review.")),
            "ReservaAceptada", Map.of(
                    "es", new Texto("Reserva aceptada", "Tu solicitud fue aceptada. Realiza el pago para confirmarla."),
                    "en", new Texto("Booking accepted", "Your request was accepted. Pay to confirm it.")),
            "ReservaRechazada", Map.of(
                    "es", new Texto("Reserva no aceptada", "El operador no pudo aceptar tu solicitud."),
                    "en", new Texto("Booking not accepted", "The operator could not accept your request.")),
            "ReservaCancelada", Map.of(
                    "es", new Texto("Reserva cancelada", "Una reserva fue cancelada."),
                    "en", new Texto("Booking cancelled", "A booking was cancelled.")),
            "PagoAprobado", Map.of(
                    "es", new Texto("Pago confirmado", "Tu pago fue aprobado y la reserva esta confirmada."),
                    "en", new Texto("Payment confirmed", "Your payment was approved and the booking is confirmed.")),
            "PagoRechazado", Map.of(
                    "es", new Texto("Pago rechazado", "La pasarela rechazo el pago. Puedes intentarlo de nuevo."),
                    "en", new Texto("Payment declined", "The gateway declined the payment. You can try again.")),
            "AlertaSeguridadEmitida", Map.of(
                    "es", new Texto("Alerta de seguridad", "Hay una alerta de seguridad en un atractivo."),
                    "en", new Texto("Safety alert", "There is a safety alert for an attraction.")),
            "AtractivoActualizado", Map.of(
                    "es", new Texto("Atractivo actualizado", "Un atractivo que sigues tuvo cambios."),
                    "en", new Texto("Attraction updated", "An attraction you follow has changed.")));

    private PlantillasNotificacion() {
    }

    static Optional<Texto> buscar(String tipo, String idioma) {
        return Optional.ofNullable(PLANTILLAS.get(tipo))
                .map(porIdioma -> porIdioma.getOrDefault(idioma, porIdioma.get("es")));
    }
}
