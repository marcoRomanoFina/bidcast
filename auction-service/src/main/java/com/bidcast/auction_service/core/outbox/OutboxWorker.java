package com.bidcast.auction_service.core.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/*
 Procesa cada evento del Outbox de forma atómica e independiente.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxWorker {

    private final RabbitTemplate rabbitTemplate;
    private final OutboxRepository outboxRepository;

    /**
     * Procesa un solo evento en su propia transacción (REQUIRES_NEW).
     * Intenta bloquear la fila individualmente (FOR UPDATE SKIP LOCKED).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(java.util.UUID eventId) {
        // 1. Intentamos bloquear la fila de forma atómica
        Optional<OutboxEvent> eventOpt = outboxRepository.findAndLockById(eventId);
        
        if (eventOpt.isEmpty()) {
            // SKIP LOCKED entró en acción: otro nodo/hilo ya tiene la fila
            return;
        }

        OutboxEvent event = eventOpt.get();

        // 2. Doble Check: ¿Otro nodo lo procesó mientras este hilo esperaba o antes de obtener el lock?
        if (event.isProcessed()) {
            log.debug("Event {} was already processed by another node. Skipping.", eventId);
            return;
        }

        try {
            event.setAttempts(event.getAttempts() + 1);
            
            // Despacho a RabbitMQ (Garantiza At-Least-Once Delivery)
            rabbitTemplate.convertAndSend(
                    event.getExchange(), 
                    event.getRoutingKey(), 
                    event.getPayload()
            );
            
            // Marcado exitoso
            event.setProcessed(true);
            event.setProcessedAt(Instant.now());
            event.setLastError(null);
            
            // Commiteamos el éxito
            outboxRepository.save(event);
            log.info("Event {} dispatched successfully.", eventId);
            
        } catch (Exception e) {
            log.error("Failed to dispatch event {}: {}", eventId, e.getMessage());
            event.setLastError(e.getMessage());
            // Guardamos el intento fallido y commiteamos la tx
            outboxRepository.save(event);
        }
    }
}
