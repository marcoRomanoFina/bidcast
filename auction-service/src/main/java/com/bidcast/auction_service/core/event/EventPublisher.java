package com.bidcast.auction_service.core.event;

/**
 * Puerto para publicar eventos de dominio.
 * En este servicio, la implementación usará el patrón Transactional Outbox.
 */
public interface EventPublisher {
    void publish(DomainEvent event);
}
