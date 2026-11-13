package com.tamaka.common.event;

/**
 * Puerto de salida de la capa de servicio. Los casos de uso publican eventos a traves de esta interfaz;
 * el adaptador concreto (HTTP interno en desarrollo, Amazon SNS en AWS) se elige por configuracion
 * en {@link EventosConfig} sin tocar la logica de negocio.
 */
public interface EventPublisher {

    void publicar(DomainEvent evento);
}
