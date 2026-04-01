package com.bidcast.auction_service.core.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Contrato base para todos los eventos de dominio del auction-service.
 */
public interface DomainEvent {
    UUID eventId();
    Instant occurredOn();
    String identifier();
}
