package com.bidcast.session_service.core.event;

import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {
    UUID eventId();
    Instant occurredOn();
    String identifier();
}
