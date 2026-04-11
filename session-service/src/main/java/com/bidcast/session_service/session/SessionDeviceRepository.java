package com.bidcast.session_service.session;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionDeviceRepository extends JpaRepository<SessionDevice, UUID> {

    Optional<SessionDevice> findBySessionIdAndDeviceId(UUID sessionId, UUID deviceId);

    long countBySessionIdAndStatus(UUID sessionId, SessionDeviceStatus status);

    List<SessionDevice> findByStatusAndLastSeenAtBefore(SessionDeviceStatus status, Instant cutoff);
}
