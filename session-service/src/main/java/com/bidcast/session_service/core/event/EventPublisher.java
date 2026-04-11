package com.bidcast.session_service.core.event;

public interface EventPublisher {
    void publish(DomainEvent event);
}
