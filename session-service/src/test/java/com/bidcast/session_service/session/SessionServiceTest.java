package com.bidcast.session_service.session;

import com.bidcast.session_service.client.SelectionClient;
import com.bidcast.session_service.client.SelectionSessionCreatedRequest;
import com.bidcast.session_service.core.event.EventPublisher;
import com.bidcast.session_service.core.exception.OpenSessionAlreadyExistsException;
import com.bidcast.session_service.core.exception.SessionDeviceNotFoundException;
import com.bidcast.session_service.core.exception.SessionNotFoundException;
import com.bidcast.session_service.session.dto.CreateSessionRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private SessionDeviceRepository sessionDeviceRepository;

    @Mock
    private SelectionClient selectionClient;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private SessionService sessionService;

    @Test
    void create_persistsWaitingSessionWithoutNotifyingSelection() {
        UUID venueId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        CreateSessionRequest request = new CreateSessionRequest(
                venueId,
                "Dinner session",
                ownerId,
                BigDecimal.valueOf(3.25)
        );

        when(sessionRepository.existsByVenueIdAndStatusIn(venueId, java.util.List.of(SessionStatus.WAITING_DEVICE, SessionStatus.ACTIVE)))
                .thenReturn(false);
        doAnswer(invocation -> {
            Session session = invocation.getArgument(0);
            return Session.builder()
                    .id(UUID.randomUUID())
                    .venueId(session.getVenueId())
                    .name(session.getName())
                    .ownerId(session.getOwnerId())
                    .basePricePerSlot(session.getBasePricePerSlot())
                    .status(session.getStatus())
                    .startedAt(session.getStartedAt())
                    .endedAt(session.getEndedAt())
                    .build();
        }).when(sessionRepository).save(any(Session.class));

        Session created = sessionService.create(request);

        assertThat(created.getStatus()).isEqualTo(SessionStatus.WAITING_DEVICE);
        assertThat(created.getStartedAt()).isNull();
        verify(selectionClient, never()).notifySessionCreated(any());
    }

    @Test
    void create_rejectsAnotherOpenSessionForSameVenue() {
        UUID venueId = UUID.randomUUID();
        CreateSessionRequest request = new CreateSessionRequest(
                venueId,
                "Dinner session",
                UUID.randomUUID(),
                BigDecimal.valueOf(3.25)
        );

        when(sessionRepository.existsByVenueIdAndStatusIn(venueId, java.util.List.of(SessionStatus.WAITING_DEVICE, SessionStatus.ACTIVE)))
                .thenReturn(true);

        assertThatThrownBy(() -> sessionService.create(request))
                .isInstanceOf(OpenSessionAlreadyExistsException.class)
                .hasMessageContaining(venueId.toString());

        verify(sessionRepository, never()).save(any(Session.class));
        verify(selectionClient, never()).notifySessionCreated(any());
    }

    @Test
    void markDeviceReady_activatesWaitingSessionAndNotifiesSelectionOnce() {
        UUID sessionId = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        Session waiting = Session.builder()
                .id(sessionId)
                .venueId(venueId)
                .name("Dinner session")
                .ownerId(ownerId)
                .basePricePerSlot(BigDecimal.valueOf(3.25))
                .build();

        when(sessionRepository.findById(sessionId)).thenReturn(java.util.Optional.of(waiting));
        when(sessionDeviceRepository.findBySessionIdAndDeviceId(sessionId, deviceId)).thenReturn(Optional.empty());
        when(sessionDeviceRepository.save(any(SessionDevice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Session activated = sessionService.markDeviceReady(sessionId, deviceId);

        assertThat(activated.getStatus()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(activated.getStartedAt()).isNotNull();
        verify(sessionDeviceRepository).save(any(SessionDevice.class));

        ArgumentCaptor<SelectionSessionCreatedRequest> notificationCaptor =
                ArgumentCaptor.forClass(SelectionSessionCreatedRequest.class);
        verify(selectionClient).notifySessionCreated(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().venueId()).isEqualTo(venueId);
        assertThat(notificationCaptor.getValue().ownerId()).isEqualTo(ownerId);
        assertThat(notificationCaptor.getValue().basePricePerSlot()).isEqualByComparingTo("3.25");
    }

    @Test
    void markDeviceReady_onlyJoinsAdditionalDeviceWhenSessionAlreadyActive() {
        UUID sessionId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        Session active = Session.builder()
                .id(sessionId)
                .venueId(UUID.randomUUID())
                .name("Dinner session")
                .ownerId(UUID.randomUUID())
                .basePricePerSlot(BigDecimal.valueOf(3.25))
                .build();
        active.activate();

        when(sessionRepository.findById(sessionId)).thenReturn(java.util.Optional.of(active));
        when(sessionDeviceRepository.findBySessionIdAndDeviceId(sessionId, deviceId)).thenReturn(Optional.empty());
        when(sessionDeviceRepository.save(any(SessionDevice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Session joined = sessionService.markDeviceReady(sessionId, deviceId);

        assertThat(joined.getStatus()).isEqualTo(SessionStatus.ACTIVE);
        verify(sessionDeviceRepository).save(any(SessionDevice.class));
        verify(selectionClient, never()).notifySessionCreated(any());
    }

    @Test
    void markDeviceReady_updatesExistingDeviceToReadyWithoutDuplicatingIt() {
        UUID sessionId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        Session active = Session.builder()
                .id(sessionId)
                .venueId(UUID.randomUUID())
                .name("Dinner session")
                .ownerId(UUID.randomUUID())
                .basePricePerSlot(BigDecimal.valueOf(3.25))
                .build();
        active.activate();

        SessionDevice existingDevice = SessionDevice.builder()
                .id(UUID.randomUUID())
                .session(active)
                .deviceId(deviceId)
                .status(SessionDeviceStatus.DISCONNECTED)
                .build();

        when(sessionRepository.findById(sessionId)).thenReturn(java.util.Optional.of(active));
        when(sessionDeviceRepository.findBySessionIdAndDeviceId(sessionId, deviceId)).thenReturn(Optional.of(existingDevice));
        when(sessionDeviceRepository.save(any(SessionDevice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Session joined = sessionService.markDeviceReady(sessionId, deviceId);

        assertThat(joined.getStatus()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(existingDevice.getStatus()).isEqualTo(SessionDeviceStatus.READY);
        verify(sessionDeviceRepository, times(1)).save(existingDevice);
        verify(selectionClient, never()).notifySessionCreated(any());
    }

    @Test
    void heartbeat_refreshesLastSeenAndKeepsDeviceReady() {
        UUID sessionId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        Session active = Session.builder()
                .id(sessionId)
                .venueId(UUID.randomUUID())
                .name("Dinner session")
                .ownerId(UUID.randomUUID())
                .basePricePerSlot(BigDecimal.valueOf(3.25))
                .build();
        active.activate();

        SessionDevice existingDevice = SessionDevice.builder()
                .id(UUID.randomUUID())
                .session(active)
                .deviceId(deviceId)
                .status(SessionDeviceStatus.DISCONNECTED)
                .build();

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(active));
        when(sessionDeviceRepository.findBySessionIdAndDeviceId(sessionId, deviceId)).thenReturn(Optional.of(existingDevice));
        when(sessionDeviceRepository.save(any(SessionDevice.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Session returnedSession = sessionService.heartbeat(sessionId, deviceId);

        assertThat(returnedSession.getId()).isEqualTo(sessionId);
        assertThat(existingDevice.getStatus()).isEqualTo(SessionDeviceStatus.READY);
        assertThat(existingDevice.getLastSeenAt()).isNotNull();
        verify(sessionDeviceRepository).save(existingDevice);
    }

    @Test
    void heartbeat_rejectsDeviceOutsideSession() {
        UUID sessionId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        Session active = Session.builder()
                .id(sessionId)
                .venueId(UUID.randomUUID())
                .name("Dinner session")
                .ownerId(UUID.randomUUID())
                .basePricePerSlot(BigDecimal.valueOf(3.25))
                .build();
        active.activate();

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(active));
        when(sessionDeviceRepository.findBySessionIdAndDeviceId(sessionId, deviceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.heartbeat(sessionId, deviceId))
                .isInstanceOf(SessionDeviceNotFoundException.class)
                .hasMessageContaining(deviceId.toString());
    }

    @Test
    void leave_marksDeviceAsLeft() {
        UUID sessionId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        Session active = Session.builder()
                .id(sessionId)
                .venueId(UUID.randomUUID())
                .name("Dinner session")
                .ownerId(UUID.randomUUID())
                .basePricePerSlot(BigDecimal.valueOf(3.25))
                .build();
        active.activate();

        SessionDevice existingDevice = SessionDevice.builder()
                .id(UUID.randomUUID())
                .session(active)
                .deviceId(deviceId)
                .status(SessionDeviceStatus.READY)
                .build();

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(active));
        when(sessionDeviceRepository.findBySessionIdAndDeviceId(sessionId, deviceId)).thenReturn(Optional.of(existingDevice));
        when(sessionDeviceRepository.save(any(SessionDevice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionDeviceRepository.existsBySessionIdAndStatus(sessionId, SessionDeviceStatus.READY)).thenReturn(true);

        Session returnedSession = sessionService.leave(sessionId, deviceId);

        assertThat(returnedSession.getStatus()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(existingDevice.getStatus()).isEqualTo(SessionDeviceStatus.LEFT);
        verify(sessionDeviceRepository).save(existingDevice);
    }

    @Test
    void leave_returnsSessionToWaitingWhenLastReadyDeviceLeaves() {
        UUID sessionId = UUID.randomUUID();
        UUID deviceId = UUID.randomUUID();
        Session active = Session.builder()
                .id(sessionId)
                .venueId(UUID.randomUUID())
                .name("Dinner session")
                .ownerId(UUID.randomUUID())
                .basePricePerSlot(BigDecimal.valueOf(3.25))
                .build();
        active.activate();

        SessionDevice existingDevice = SessionDevice.builder()
                .id(UUID.randomUUID())
                .session(active)
                .deviceId(deviceId)
                .status(SessionDeviceStatus.READY)
                .build();

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(active));
        when(sessionDeviceRepository.findBySessionIdAndDeviceId(sessionId, deviceId)).thenReturn(Optional.of(existingDevice));
        when(sessionDeviceRepository.save(any(SessionDevice.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(sessionDeviceRepository.existsBySessionIdAndStatus(sessionId, SessionDeviceStatus.READY)).thenReturn(false);
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Session returnedSession = sessionService.leave(sessionId, deviceId);

        assertThat(returnedSession.getStatus()).isEqualTo(SessionStatus.WAITING_DEVICE);
        assertThat(existingDevice.getStatus()).isEqualTo(SessionDeviceStatus.LEFT);
        verify(sessionRepository).save(active);
    }

    @Test
    void close_marksSessionAsClosedAndPublishesLifecycleEvent() {
        UUID sessionId = UUID.randomUUID();
        Session existing = Session.builder()
                .id(sessionId)
                .venueId(UUID.randomUUID())
                .name("Dinner session")
                .ownerId(UUID.randomUUID())
                .basePricePerSlot(BigDecimal.valueOf(3.25))
                .build();
        existing.activate();

        when(sessionRepository.findById(sessionId)).thenReturn(java.util.Optional.of(existing));
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Session closed = sessionService.close(sessionId);

        assertThat(closed.getStatus()).isEqualTo(SessionStatus.CLOSED);
        assertThat(closed.getEndedAt()).isNotNull();
        assertThat(closed.getClosedReason()).isEqualTo(SessionClosedReason.MANUAL);
        verify(eventPublisher).publish(any(SessionClosedEvent.class));
    }

    @Test
    void close_rejectsMissingSession() {
        UUID sessionId = UUID.randomUUID();
        when(sessionRepository.findById(sessionId)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> sessionService.close(sessionId))
                .isInstanceOf(SessionNotFoundException.class)
                .hasMessageContaining(sessionId.toString());
    }
}
