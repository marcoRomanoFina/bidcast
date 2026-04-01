package com.bidcast.auction_service.core.outbox;

import org.junit.jupiter.api.BeforeEach;
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

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private OutboxWorker outboxWorker;

    @InjectMocks
    private OutboxRelay outboxRelay;

    private OutboxEvent event;

    @BeforeEach
    void setUp() {
        event = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .exchange("test-exchange")
                .routingKey("test-key")
                .payload("{}")
                .build();
    }

    @Test
    void scheduleDispatch_whenNoPendingEvents_doesNothing() {
        when(outboxRepository.findPendingBatchAndLock(any())).thenReturn(Collections.emptyList());

        outboxRelay.scheduleDispatch();

        verify(outboxWorker, never()).process(any());
    }

    @Test
    void scheduleDispatch_whenPendingEvents_callsWorker() {
        when(outboxRepository.findPendingBatchAndLock(any())).thenReturn(List.of(event));

        outboxRelay.scheduleDispatch();

        verify(outboxWorker, times(1)).process(event);
    }

    @Test
    void scheduleDispatch_limitTo50Events() {
        when(outboxRepository.findPendingBatchAndLock(PageRequest.of(0, 50))).thenReturn(Collections.emptyList());

        outboxRelay.scheduleDispatch();

        verify(outboxRepository).findPendingBatchAndLock(PageRequest.of(0, 50));
    }
}
