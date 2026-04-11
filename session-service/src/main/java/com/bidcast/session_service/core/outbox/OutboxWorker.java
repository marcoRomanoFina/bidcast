package com.bidcast.session_service.core.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxWorker {

    private final RabbitTemplate rabbitTemplate;
    private final OutboxRepository outboxRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(OutboxEvent event) {
        try {
            rabbitTemplate.convertAndSend(
                    event.getExchange(),
                    event.getRoutingKey(),
                    event.getPayload()
            );
        } catch (Exception e) {
            log.error("Failed to dispatch outbox event {}: {}", event.getId(), e.getMessage());
            markFailed(event, e);
            return;
        }

        markProcessed(event);
    }

    private void markProcessed(OutboxEvent event) {
        try {
            event.setAttempts(event.getAttempts() + 1);
            event.setProcessed(true);
            event.setProcessedAt(Instant.now());
            event.setLastError(null);
            outboxRepository.saveAndFlush(event);
        } catch (Exception e) {
            log.error("Outbox event {} was published but could not be marked as processed.", event.getId(), e);
            throw e;
        }
    }

    private void markFailed(OutboxEvent event, Exception cause) {
        event.setAttempts(event.getAttempts() + 1);
        event.setProcessed(false);
        event.setProcessedAt(null);
        event.setLastError(truncateError(cause.getMessage()));
        outboxRepository.saveAndFlush(event);
    }

    private String truncateError(String error) {
        if (error == null || error.isBlank()) {
            return "Unknown outbox dispatch error";
        }
        int maxLength = 1000;
        return error.length() <= maxLength ? error : error.substring(0, maxLength);
    }
}
