package com.bidcast.auction_service.core.outbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
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
                .exchange("test-exchange")
                .routingKey("test-key")
                .payload("{\"key\":\"value\"}")
                .build();
    }

    @Test
    void process_whenSuccess_sendsToRabbitAndMarksAsProcessed() {
        outboxWorker.process(event);

        verify(rabbitTemplate).convertAndSend(eq("test-exchange"), eq("test-key"), eq("{\"key\":\"value\"}"));
        verify(outboxRepository).save(argThat(e -> e.getProcessedAt() != null));
    }

    @Test
    void process_whenRabbitFails_savesError() {
        doThrow(new RuntimeException("Rabbit down")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), anyString());

        outboxWorker.process(event);

        verify(outboxRepository).save(argThat(e -> e.getLastError() != null && e.getLastError().contains("Rabbit down")));
    }
}
