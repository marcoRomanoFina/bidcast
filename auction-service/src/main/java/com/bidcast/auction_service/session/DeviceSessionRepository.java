package com.bidcast.auction_service.session;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface DeviceSessionRepository extends JpaRepository<DeviceSession, String> {

    @Query("SELECT s FROM DeviceSession s WHERE s.status = 'ACTIVE' AND s.startedAt < :olderThan")
    Slice<DeviceSession> findStaleActiveSessions(Instant olderThan, Pageable pageable);
}
