package com.bidcast.selection_service.core.outbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxWorkerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private OutboxRepository outboxRepository;

    @InjectMocks
    private OutboxWorker outboxWorker;

    private OutboxEvent event;

    @BeforeEach
    void setUp() {
        event = OutboxEvent.builder()
                .id(UUID.randomUUID())
                .eventId(UUID.randomUUID())
                .exchange("test-exchange")
                .routingKey("test-key")
                .payload("{\"key\":\"value\"}")
                .build();
    }

    @Test
    void process_whenSuccess_sendsToRabbitAndMarksAsProcessed() {
        outboxWorker.process(event);

        verify(rabbitTemplate).convertAndSend(eq("test-exchange"), eq("test-key"), eq("{\"key\":\"value\"}"));
        verify(outboxRepository).saveAndFlush(argThat(e ->
                e.isProcessed()
                        && e.getProcessedAt() != null
                        && e.getAttempts() == 1
                        && e.getLastError() == null
        ));
    }

    @Test
    void process_whenRabbitFails_savesError() {
        doThrow(new RuntimeException("Rabbit down")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), anyString());

        outboxWorker.process(event);

        verify(outboxRepository).saveAndFlush(argThat(e ->
                !e.isProcessed()
                        && e.getProcessedAt() == null
                        && e.getAttempts() == 1
                        && e.getLastError() != null
                        && e.getLastError().contains("Rabbit down")
        ));
    }

    @Test
    void process_whenMarkProcessedFails_propagatesAfterPublishing() {
        doThrow(new RuntimeException("DB down")).when(outboxRepository).saveAndFlush(any(OutboxEvent.class));

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> outboxWorker.process(event));

        verify(rabbitTemplate).convertAndSend(eq("test-exchange"), eq("test-key"), eq("{\"key\":\"value\"}"));
        verify(outboxRepository, times(1)).saveAndFlush(any(OutboxEvent.class));
    }
}
