package com.bidcast.auction_service.event;

import com.bidcast.auction_service.core.event.DomainEvent;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;

/**
 * Evento que indica que una sesión de dispositivo ha finalizado.
 */
public record SessionClosedEvent(
    UUID eventId,
    Instant occurredOn,
    
    @NotBlank(message = "Session id is required")
    String sessionId,
    
    @NotBlank(message = "Publisher id is required")
    String publisherId
) implements DomainEvent {

    public SessionClosedEvent(String sessionId, String publisherId) {
        this(UUID.randomUUID(), Instant.now(), sessionId, publisherId);
    }

    @Override
    public String identifier() { return sessionId; }
}
