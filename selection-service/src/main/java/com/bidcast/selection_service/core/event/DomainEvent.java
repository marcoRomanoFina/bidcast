package com.bidcast.selection_service.core.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Contrato base para todos los eventos de dominio del selection-service.
 */
public interface DomainEvent {
    UUID eventId();
    Instant occurredOn();
    String identifier();
}
