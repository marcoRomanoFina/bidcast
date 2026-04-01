package com.bidcast.auction_service.core.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * RESPONSABILIDAD: Polling periódico de la tabla Outbox.
 * Selecciona lotes de eventos no procesados usando SKIP LOCKED para escalabilidad horizontal.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRelay {

    private final OutboxRepository outboxRepository;
    private final OutboxWorker outboxWorker;

    /**
     * Poller de eventos.
     * Abre una transacción por lote y bloquea las filas para que otros nodos las ignoren.
     */
    @Scheduled(fixedDelayString = "${bidcast.outbox.polling-delay:5000}")
    @Transactional
    public void scheduleDispatch() {
        // 1. Buscamos y bloqueamos 50 eventos (SKIP LOCKED)
        List<OutboxEvent> pending = outboxRepository.findPendingBatchAndLock(PageRequest.of(0, 50));
        
        if (pending.isEmpty()) {
            return;
        }

        log.info("Outbox Relay: Processing batch of {} events.", pending.size());

        // 2. Despachamos el lote
        pending.forEach(outboxWorker::process);
    }
}
