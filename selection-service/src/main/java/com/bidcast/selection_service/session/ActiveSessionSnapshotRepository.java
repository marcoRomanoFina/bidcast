package com.bidcast.selection_service.session;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ActiveSessionSnapshotRepository extends JpaRepository<ActiveSessionSnapshot, String> {

    Optional<ActiveSessionSnapshot> findBySessionIdAndStatus(String sessionId, ActiveSessionStatus status);

    List<ActiveSessionSnapshot> findByStatus(ActiveSessionStatus status);
}
