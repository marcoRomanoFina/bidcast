package com.bidcast.auction_service.core.outbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock private OutboxRepository outboxRepository;
    @Mock private OutboxWorker outboxWorker;

    @InjectMocks
    private OutboxRelay outboxRelay;

    private OutboxEvent event;

    @BeforeEach
    void setUp() {
        event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setExchange("wallet.exchange");
        event.setRoutingKey("wallet.settlement");
        event.setPayload("{\"spentAmount\":10.0}");
        event.setProcessed(false);
    }

    @Test
    @DisplayName("Relay: Encuentra eventos y delega el procesamiento al Worker")
    void scheduleDispatch_DelegatesToWorker() {
        // Arrange
        when(outboxRepository.findPending(any(PageRequest.class)))
                .thenReturn(List.of(event));

        // Act
        outboxRelay.scheduleDispatch();

        // Assert
        verify(outboxRepository).findPending(any(PageRequest.class));
        verify(outboxWorker).process(event.getId());
    }

    @Test
    @DisplayName("Relay: Sin eventos pendientes no llama al Worker")
    void scheduleDispatch_NoEvents_DoesNothing() {
        // Arrange
        when(outboxRepository.findPending(any(PageRequest.class)))
                .thenReturn(Collections.emptyList());

        // Act
        outboxRelay.scheduleDispatch();

        // Assert
        verify(outboxRepository).findPending(any(PageRequest.class));
        verifyNoInteractions(outboxWorker);
    }

    @Test
    @DisplayName("Relay: Batch Process delega múltiples llamadas al Worker")
    void scheduleDispatch_Batch_DelegatesMultipleTimes() {
        // Arrange
        OutboxEvent event2 = new OutboxEvent();
        event2.setId(UUID.randomUUID());
        
        when(outboxRepository.findPending(any(PageRequest.class)))
                .thenReturn(List.of(event, event2));

        // Act
        outboxRelay.scheduleDispatch();

        // Assert
        verify(outboxWorker, times(2)).process(any(java.util.UUID.class));
    }
}
