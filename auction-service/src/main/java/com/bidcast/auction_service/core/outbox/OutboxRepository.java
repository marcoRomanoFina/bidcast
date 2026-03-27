package com.bidcast.auction_service.core.outbox;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {
    
    /**
     * Selecciona eventos pendientes para procesamiento por lote, priorizando los más antiguos.
     */
    @Query("SELECT e FROM OutboxEvent e WHERE e.processed = false AND e.attempts < 5 ORDER BY e.createdAt ASC")
    List<OutboxEvent> findPending(Pageable pageable);

    /**
     * Intenta bloquear un evento específico. Si está bloqueado por otro, SKIP LOCKED lo ignorará (retornará Optional.empty).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")})
    @Query("SELECT e FROM OutboxEvent e WHERE e.id = :id")
    Optional<OutboxEvent> findAndLockById(UUID id);
}
