package com.bidcast.auction_service.event;

import com.bidcast.auction_service.core.event.DomainEvent;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.UUID;

/**
 * Evento que indica que una sesión de dispositivo ha comenzado.
 */
public record SessionStartedEvent(
    UUID eventId,
    Instant occurredOn,
    
    @NotBlank(message = "Session ID is required")
    String sessionId,
    
    @NotBlank(message = "Device ID is required")
    String deviceId,
    
    @NotBlank(message = "Publisher ID is required")
    String publisherId
) implements DomainEvent {

    public SessionStartedEvent(String sessionId, String deviceId, String publisherId) {
        this(UUID.randomUUID(), Instant.now(), sessionId, deviceId, publisherId);
    }

    @Override
    public String identifier() { return sessionId; }
}
