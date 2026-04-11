package com.bidcast.session_service.session;

import com.bidcast.session_service.client.SelectionClient;
import com.bidcast.session_service.client.SelectionSessionCreatedRequest;
import com.bidcast.session_service.core.event.EventPublisher;
import com.bidcast.session_service.core.exception.OpenSessionAlreadyExistsException;
import com.bidcast.session_service.core.exception.SessionDeviceNotFoundException;
import com.bidcast.session_service.core.exception.SessionNotFoundException;
import com.bidcast.session_service.session.dto.CreateSessionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionService {

    private static final List<SessionStatus> OPEN_STATUSES = List.of(SessionStatus.WAITING_DEVICE, SessionStatus.ACTIVE);

    private final SessionRepository sessionRepository;
    private final SessionDeviceRepository sessionDeviceRepository;
    private final SelectionClient selectionClient;
    private final EventPublisher eventPublisher;

    @Transactional
    public Session create(CreateSessionRequest request) {
        if (sessionRepository.existsByVenueIdAndStatusIn(request.venueId(), OPEN_STATUSES)) {
            throw new OpenSessionAlreadyExistsException(request.venueId());
        }

        Session session = Session.builder()
                .venueId(request.venueId())
                .name(request.name())
                .ownerId(request.ownerId())
                .basePricePerSlot(request.basePricePerSlot())
                .build();

        return sessionRepository.save(session);
    }

    @Transactional
    public Session markDeviceReady(UUID sessionId, UUID deviceId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));

        SessionDevice sessionDevice = sessionDeviceRepository.findBySessionIdAndDeviceId(sessionId, deviceId)
                .map(existingDevice -> {
                    existingDevice.markReady();
                    return existingDevice;
                })
                .orElseGet(() -> SessionDevice.builder()
                        .session(session)
                        .deviceId(deviceId)
                        .build());
        sessionDevice.markReady();
        sessionDeviceRepository.save(sessionDevice);

        if (session.isWaitingForDevice()) { // proximamente state pattern
            session.activate();
            Session persisted = sessionRepository.save(session);

            selectionClient.notifySessionCreated(new SelectionSessionCreatedRequest(
                    persisted.getId().toString(),
                    persisted.getVenueId(),
                    persisted.getOwnerId(),
                    persisted.getBasePricePerSlot()
            ));

            return persisted;
        }

        return session;
    }

    @Transactional
    public Session heartbeat(UUID sessionId, UUID deviceId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));

        SessionDevice sessionDevice = sessionDeviceRepository.findBySessionIdAndDeviceId(sessionId, deviceId)
                .orElseThrow(() -> new SessionDeviceNotFoundException(sessionId, deviceId));

        sessionDevice.recordHeartbeat();
        sessionDeviceRepository.save(sessionDevice);

        return session;
    }

    @Transactional
    public Session leave(UUID sessionId, UUID deviceId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));

        SessionDevice sessionDevice = sessionDeviceRepository.findBySessionIdAndDeviceId(sessionId, deviceId)
                .orElseThrow(() -> new SessionDeviceNotFoundException(sessionId, deviceId));

        sessionDevice.leave();
        sessionDeviceRepository.save(sessionDevice);

        if (session.isActive() && sessionDeviceRepository.countBySessionIdAndStatus(sessionId, SessionDeviceStatus.READY) == 0) {
            session.waitForDevice();
            return sessionRepository.save(session);
        }

        return session;
    }

    @Transactional
    public Session close(UUID sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundException(sessionId));

        session.close();
        Session persisted = sessionRepository.save(session);

        eventPublisher.publish(new SessionClosedEvent(
                persisted.getId().toString(),
                persisted.getEndedAt()
        ));

        return persisted;
    }
}
