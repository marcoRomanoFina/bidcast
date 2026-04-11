package com.bidcast.session_service.session;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {

    boolean existsByVenueIdAndStatusIn(UUID venueId, Collection<SessionStatus> statuses);

    List<Session> findByStatusAndUpdatedAtBefore(SessionStatus status, Instant cutoff);
}
