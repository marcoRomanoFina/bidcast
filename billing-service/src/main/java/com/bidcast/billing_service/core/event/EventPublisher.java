package com.bidcast.billing_service.core.event;

/**
 * Puerto genérico para la publicación de eventos de dominio.
 * Desacopla la lógica de negocio de la infraestructura de mensajería (RabbitMQ, Kafka, etc).
 */
public interface EventPublisher {
    void publish(DomainEvent event);
}
