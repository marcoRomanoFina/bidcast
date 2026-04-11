package com.bidcast.session_service.core.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxRelay {

    static final int BATCH_SIZE = 50;
    static final int MAX_ATTEMPTS = 10;

    private final OutboxRepository outboxRepository;
    private final OutboxWorker outboxWorker;

    @Scheduled(fixedDelayString = "${adcast.outbox.polling-delay:5000}")
    @Transactional
    public void scheduleDispatch() {
        List<OutboxEvent> pending = outboxRepository.findPendingBatchAndLock(PageRequest.of(0, BATCH_SIZE), MAX_ATTEMPTS);

        if (pending.isEmpty()) {
            return;
        }

        log.info("Outbox Relay: Processing batch of {} events.", pending.size());
        pending.forEach(outboxWorker::process);
    }
}
