package com.bidcast.auction_service.infrastructure.event;

import com.bidcast.auction_service.config.RabbitMQConfig;
import com.bidcast.auction_service.core.event.DomainEvent;
import com.bidcast.auction_service.core.event.EventPublisher;
import com.bidcast.auction_service.core.outbox.OutboxEvent;
import com.bidcast.auction_service.core.outbox.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Adaptador de infraestructura que implementa el EventPublisher usando el patrón Outbox.
 * Todo evento nacido en este servicio se publica en el EXCHANGE_AUCTION.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventPublisher implements EventPublisher {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void publish(DomainEvent event) {
        String eventName = event.getClass().getSimpleName();
        // El servicio es dueño de sus eventos, siempre publica en su propio exchange
        String exchange = RabbitMQConfig.EXCHANGE_AUCTION;
        String routingKey = "event." + eventName.toLowerCase();

        try {
            String payload = objectMapper.writeValueAsString(event);
            
            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateId(event.identifier())
                    .exchange(exchange)
                    .routingKey(routingKey)
                    .payload(payload)
                    .build();

            outboxRepository.save(outboxEvent);
            log.info("Domain event {} stored in Outbox for auction.exchange", eventName);
            
        } catch (Exception e) {
            log.error("Failed to persist domain event to Outbox: {}", eventName, e);
            throw new RuntimeException("Event persistence failed", e);
        }
    }
}
