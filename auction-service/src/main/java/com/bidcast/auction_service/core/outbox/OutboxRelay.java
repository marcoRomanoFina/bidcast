package com.bidcast.auction_service.core.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;


import java.util.List;

/**
 * RELAY: El encargado de garantizar la entrega de eventos (at-least-once delivery).
 * Realiza el Polling de la tabla y delega el procesamiento al Worker.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxRelay {

    private final OutboxRepository outboxRepository;
    private final OutboxWorker outboxWorker;

    /**
     * Polling de la tabla Outbox. Utiliza el repositorio con SKIP LOCKED.
     * MANTENEMOS @Transactional para que los candados de fila vivan durante 
     * todo el procesamiento del lote, evitando duplicados en otros nodos.
     */
    @Scheduled(fixedDelayString = "${bidcast.outbox.polling-delay:5000}")
    public void scheduleDispatch() {
        // Consultamos candidatos sin bloquear la tabla
        List<OutboxEvent> pending = outboxRepository.findPending(PageRequest.of(0, 50));
        
        if (pending.isEmpty()) return;

        log.info("Starting batch dispatch: {} candidates found.", pending.size());
        
        // El Worker intentará bloquear cada evento de forma independiente
        pending.forEach(event -> outboxWorker.process(event.getId()));
        
        log.info("Batch dispatch finished.");
    }
}
