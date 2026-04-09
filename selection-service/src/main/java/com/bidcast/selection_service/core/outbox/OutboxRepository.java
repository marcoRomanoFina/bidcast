package com.bidcast.selection_service.core.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {

/*SELECT id, event_id, aggregate_id, exchange, routing_key, payload, processed, created_at, ... 
FROM outbox_event 
WHERE processed = false
  AND attempts < :maxAttempts
ORDER BY created_at ASC 
LIMIT 50 
FOR UPDATE SKIP LOCKED; */

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")})
    @Query("SELECT e FROM OutboxEvent e WHERE e.processed = false AND e.attempts < :maxAttempts ORDER BY e.createdAt ASC")
    List<OutboxEvent> findPendingBatchAndLock(Pageable pageable, int maxAttempts);
}
