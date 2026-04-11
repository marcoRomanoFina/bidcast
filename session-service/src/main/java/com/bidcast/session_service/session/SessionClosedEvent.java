package com.bidcast.session_service.session;

import com.bidcast.session_service.core.event.DomainEvent;

import java.time.Instant;
import java.util.UUID;

public record SessionClosedEvent(
        UUID eventId,
        Instant occurredOn,
        String sessionId,
        Instant closedAt
) implements DomainEvent {

    public SessionClosedEvent(String sessionId, Instant closedAt) {
        this(UUID.randomUUID(), Instant.now(), sessionId, closedAt);
    }

    @Override
    public String identifier() {
        return sessionId;
    }
}
