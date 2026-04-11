package com.bidcast.selection_service.session;

import com.bidcast.selection_service.offer.OfferInfrastructureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SelectionSessionRehydrationService {

    private final ActiveSessionSnapshotRepository snapshotRepository;
    private final OfferInfrastructureService infrastructureService;

    public void rehydrateAllActiveSessions() {
        snapshotRepository.findByStatus(ActiveSessionStatus.ACTIVE)
                .forEach(this::rehydrate);
    }

    public void rehydrateSession(String sessionId) {
        snapshotRepository.findByStatusAndSessionId(ActiveSessionStatus.ACTIVE, sessionId)
                .ifPresent(this::rehydrate);
    }

    private void rehydrate(ActiveSessionSnapshot snapshot) {
        infrastructureService.initializeSessionContext(
                snapshot.getSessionId(),
                snapshot.getVenueId(),
                snapshot.getOwnerId(),
                snapshot.getBasePricePerSlot().toPlainString()
        );
        log.debug("Session {} rehydrated into Redis hot state", snapshot.getSessionId());
    }
}
