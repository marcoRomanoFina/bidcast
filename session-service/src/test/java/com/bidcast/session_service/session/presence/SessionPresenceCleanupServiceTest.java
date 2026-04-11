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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionPresenceCleanupServiceTest {

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private SessionDeviceRepository sessionDeviceRepository;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private SessionPresenceCleanupService sessionPresenceCleanupService;

    @Test
    void cleanup_marksStaleDevicesDisconnectedAndReturnsSessionToWaiting() {
        SessionPresenceProperties properties = new SessionPresenceProperties();
        properties.setStaleAfter(Duration.ofSeconds(60));
        properties.setCloseEmptyAfter(Duration.ofMinutes(5));
        sessionPresenceCleanupService = new SessionPresenceCleanupService(
                sessionRepository,
                sessionDeviceRepository,
                properties,
                eventPublisher
        );

        Session session = Session.builder()
                .id(UUID.randomUUID())
                .venueId(UUID.randomUUID())
                .name("Dinner session")
                .ownerId(UUID.randomUUID())
                .basePricePerSlot(BigDecimal.valueOf(3.25))
                .status(SessionStatus.ACTIVE)
                .build();

        SessionDevice staleDevice = SessionDevice.builder()
                .id(UUID.randomUUID())
                .session(session)
                .deviceId(UUID.randomUUID())
                .status(SessionDeviceStatus.READY)
                .lastSeenAt(Instant.now().minusSeconds(120))
                .build();

        when(sessionDeviceRepository.findByStatusAndLastSeenAtBefore(any(), any())).thenReturn(List.of(staleDevice));
        when(sessionDeviceRepository.existsBySessionIdAndStatus(session.getId(), SessionDeviceStatus.READY)).thenReturn(false);
        when(sessionRepository.findByStatusAndUpdatedAtBefore(any(), any())).thenReturn(List.of());

        sessionPresenceCleanupService.cleanup();

        assertThat(staleDevice.getStatus()).isEqualTo(SessionDeviceStatus.DISCONNECTED);
        assertThat(session.getStatus()).isEqualTo(SessionStatus.WAITING_DEVICE);
        verify(sessionRepository).save(session);
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void cleanup_closesWaitingSessionsWithoutReadyDevices() {
        SessionPresenceProperties properties = new SessionPresenceProperties();
        properties.setStaleAfter(Duration.ofSeconds(60));
        properties.setCloseEmptyAfter(Duration.ofMinutes(5));
        sessionPresenceCleanupService = new SessionPresenceCleanupService(
                sessionRepository,
                sessionDeviceRepository,
                properties,
                eventPublisher
        );

        Session waitingSession = Session.builder()
                .id(UUID.randomUUID())
                .venueId(UUID.randomUUID())
                .name("Dinner session")
                .ownerId(UUID.randomUUID())
                .basePricePerSlot(BigDecimal.valueOf(3.25))
                .build();

        when(sessionDeviceRepository.findByStatusAndLastSeenAtBefore(any(), any())).thenReturn(List.of());
        when(sessionRepository.findByStatusAndUpdatedAtBefore(eq(SessionStatus.WAITING_DEVICE), any())).thenReturn(List.of(waitingSession));
        when(sessionDeviceRepository.existsBySessionIdAndStatus(waitingSession.getId(), SessionDeviceStatus.READY)).thenReturn(false);
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));

        sessionPresenceCleanupService.cleanup();

        assertThat(waitingSession.getStatus()).isEqualTo(SessionStatus.CLOSED);
        assertThat(waitingSession.getClosedReason()).isEqualTo(SessionClosedReason.NO_DEVICES);
        verify(eventPublisher).publish(any(SessionClosedEvent.class));
    }
}
