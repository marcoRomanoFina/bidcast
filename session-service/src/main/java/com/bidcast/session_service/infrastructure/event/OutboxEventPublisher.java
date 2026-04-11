package com.bidcast.session_service.infrastructure.event;

import com.bidcast.session_service.config.RabbitMQConfig;
import com.bidcast.session_service.core.event.DomainEvent;
import com.bidcast.session_service.core.event.EventPublisher;
import com.bidcast.session_service.core.outbox.OutboxEvent;
import com.bidcast.session_service.core.outbox.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublisher implements EventPublisher {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(DomainEvent event) {
        String eventName = event.getClass().getSimpleName();
        String exchange = RabbitMQConfig.EXCHANGE_SESSION;
        String routingKey = "event." + eventName.toLowerCase();

        try {
            String payload = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .eventId(event.eventId())
                    .aggregateId(event.identifier())
                    .exchange(exchange)
                    .routingKey(routingKey)
                    .payload(payload)
                    .build();

            outboxRepository.save(outboxEvent);
            log.info("Domain event {} stored in Outbox for session.exchange", eventName);
        } catch (Exception e) {
            log.error("Failed to persist domain event to Outbox: {}", eventName, e);
            throw new RuntimeException("Event persistence failed", e);
        }
    }
}
