package com.bidcast.selection_service.session;

import com.bidcast.selection_service.core.exception.SessionContextNotFoundException;
import com.bidcast.selection_service.offer.OfferInfrastructureService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SelectionSessionService {

    private final OfferInfrastructureService infrastructureService;
    private final ActiveSessionSnapshotRepository snapshotRepository;

    @Transactional
    public void register(SelectionSessionRegistrationRequest request) {
        ActiveSessionSnapshot snapshot = snapshotRepository.findById(request.sessionId())
                .map(existing -> {
                    existing.refresh(
                            request.venueId().toString(),
                            request.ownerId().toString(),
                            request.basePricePerSlot()
                    );
                    return existing;
                })
                .orElseGet(() -> ActiveSessionSnapshot.builder()
                        .sessionId(request.sessionId())
                        .venueId(request.venueId().toString())
                        .ownerId(request.ownerId().toString())
                        .basePricePerSlot(request.basePricePerSlot())
                        .status(ActiveSessionStatus.ACTIVE)
                        .build());

        snapshotRepository.save(snapshot);

        infrastructureService.initializeSessionContext(
                request.sessionId(),
                request.venueId().toString(),
                request.ownerId().toString(),
                request.basePricePerSlot().toPlainString()
        );
    }

    @Transactional(readOnly = true)
    public ActiveSessionSnapshot getRequiredActiveSession(String sessionId) {
        return snapshotRepository.findByStatusAndSessionId(ActiveSessionStatus.ACTIVE, sessionId)
                .orElseThrow(() -> new SessionContextNotFoundException(sessionId));
    }

    @Transactional
    public void close(String sessionId) {
        Optional<ActiveSessionSnapshot> snapshot = snapshotRepository.findById(sessionId);
        if (snapshot.isEmpty()) {
            infrastructureService.purgeSessionContext(sessionId);
            return;
        }

        snapshot.get().close();
        infrastructureService.purgeSessionContext(sessionId);
    }
}
