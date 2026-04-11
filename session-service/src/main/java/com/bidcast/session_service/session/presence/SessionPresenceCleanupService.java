package com.bidcast.session_service.session.presence;

import com.bidcast.session_service.core.event.EventPublisher;
import com.bidcast.session_service.session.Session;
import com.bidcast.session_service.session.SessionClosedEvent;
import com.bidcast.session_service.session.SessionClosedReason;
import com.bidcast.session_service.session.SessionDevice;
import com.bidcast.session_service.session.SessionDeviceRepository;
import com.bidcast.session_service.session.SessionDeviceStatus;
import com.bidcast.session_service.session.SessionRepository;
import com.bidcast.session_service.session.SessionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionPresenceCleanupService {

    private final SessionRepository sessionRepository;
    private final SessionDeviceRepository sessionDeviceRepository;
    private final SessionPresenceProperties sessionPresenceProperties;
    private final EventPublisher eventPublisher;

    @Transactional
    public void cleanup() {
        Instant now = Instant.now();
        // Primero enfriamos devices stale, despues revisamos sessions que quedaron esperando demasiado.
        markStaleDevices(now.minus(sessionPresenceProperties.getStaleAfter()));
        closeWaitingSessionsWithoutDevices(now.minus(sessionPresenceProperties.getCloseEmptyAfter()));
    }

    private void markStaleDevices(Instant staleCutoff) {
        List<SessionDevice> staleDevices = sessionDeviceRepository.findByStatusAndLastSeenAtBefore(SessionDeviceStatus.READY, staleCutoff);

        for (SessionDevice staleDevice : staleDevices) {
            staleDevice.markDisconnected();
            sessionDeviceRepository.save(staleDevice);

            Session session = staleDevice.getSession();
            // Cuando no queda ningun READY, la session activa vuelve a WAITING_DEVICE.
            if (session.isActive() && sessionDeviceRepository.countBySessionIdAndStatus(session.getId(), SessionDeviceStatus.READY) == 0) {
                session.waitForDevice();
                sessionRepository.save(session);
                log.info("Session {} returned to WAITING_DEVICE after last ready device became stale", session.getId());
            }
        }
    }

    private void closeWaitingSessionsWithoutDevices(Instant waitingCutoff) {
        List<Session> waitingSessions = sessionRepository.findByStatusAndUpdatedAtBefore(SessionStatus.WAITING_DEVICE, waitingCutoff);

        for (Session session : waitingSessions) {
            if (sessionDeviceRepository.countBySessionIdAndStatus(session.getId(), SessionDeviceStatus.READY) > 0) {
                continue;
            }

            // Si nadie volvio a conectarse dentro de la ventana, cerramos por ausencia de devices.
            session.close(SessionClosedReason.NO_DEVICES);
            Session persisted = sessionRepository.save(session);

            eventPublisher.publish(new SessionClosedEvent(
                    persisted.getId().toString(),
                    persisted.getEndedAt()
            ));

            log.info("Session {} closed automatically due to missing devices", persisted.getId());
        }
    }
}
