package com.bidcast.selection_service.core.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * RESPONSABILIDAD: Ejecutar el despacho físico del evento a RabbitMQ.
 * Recibe el evento ya bloqueado por el Relay para evitar colisiones.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxWorker {

    private final RabbitTemplate rabbitTemplate;
    private final OutboxRepository outboxRepository;

    /**
     * Procesa un evento en su propia transacción para que un fallo no contamine el resto del batch.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(OutboxEvent event) {
        try {
            log.debug("Dispatching event {} to {}/{}", event.getId(), event.getExchange(), event.getRoutingKey());

            // Despacho a RabbitMQ
            rabbitTemplate.convertAndSend(
                    event.getExchange(),
                    event.getRoutingKey(),
                    event.getPayload()
            );
        } catch (Exception e) {
            log.error("Failed to dispatch event {}: {}", event.getId(), e.getMessage());
            markFailed(event, e);
            return;
        }

        markProcessed(event);
    }

    private void markProcessed(OutboxEvent event) {
        try {
            event.setAttempts(event.getAttempts() + 1);
            event.setProcessedAt(Instant.now());
            event.setProcessed(true);
            event.setLastError(null);
            outboxRepository.saveAndFlush(event);
        } catch (Exception e) {
            log.error(
                    "Event {} was published to RabbitMQ but could not be marked as processed in the outbox.",
                    event.getId(),
                    e
            );
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
