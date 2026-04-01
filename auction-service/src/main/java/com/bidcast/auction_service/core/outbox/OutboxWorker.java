package com.bidcast.auction_service.core.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

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
     * Procesa y despacha un evento individual dentro de la transacción del lote.
     */
    public void process(OutboxEvent event) {
        try {
            log.debug("Dispatching event {} to {}/{}", event.getId(), event.getExchange(), event.getRoutingKey());

            // Despacho a RabbitMQ
            rabbitTemplate.convertAndSend(
                    event.getExchange(),
                    event.getRoutingKey(),
                    event.getPayload()
            );

            // Marcamos como procesado
            event.setProcessedAt(Instant.now());
            event.setProcessed(true); // <--- Faltaba esto
            event.setLastError(null);
            outboxRepository.save(event);

        } catch (Exception e) {
            log.error("Failed to dispatch event {}: {}", event.getId(), e.getMessage());
            event.setLastError(e.getMessage());
            outboxRepository.save(event);
        }
    }
}
