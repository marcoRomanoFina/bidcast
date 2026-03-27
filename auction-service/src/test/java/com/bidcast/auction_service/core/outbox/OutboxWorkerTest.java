package com.bidcast.auction_service.core.outbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxWorkerTest {

    @Mock private RabbitTemplate rabbitTemplate;
    @Mock private OutboxRepository outboxRepository;

    @InjectMocks
    private OutboxWorker outboxWorker;

    private OutboxEvent event;

    @BeforeEach
    void setUp() {
        event = new OutboxEvent();
        event.setId(UUID.randomUUID());
        event.setExchange("wallet.exchange");
        event.setRoutingKey("wallet.settlement");
        event.setPayload("{\"spentAmount\":10.0}");
        event.setAttempts(0);
        event.setProcessed(false);
    }

    @Test
    @DisplayName("Worker: Procesa exitosamente, envía a Rabbit y marca como procesado")
    void process_Success() {
        // Arrange
        when(outboxRepository.findAndLockById(event.getId())).thenReturn(java.util.Optional.of(event));

        // Act
        outboxWorker.process(event.getId());

        // Assert
        verify(rabbitTemplate).convertAndSend(eq("wallet.exchange"), eq("wallet.settlement"), eq("{\"spentAmount\":10.0}"));
        assertTrue(event.isProcessed());
        assertEquals(1, event.getAttempts());
        assertNull(event.getLastError());
        assertNotNull(event.getProcessedAt());
        verify(outboxRepository).save(event);
    }

    @Test
    @DisplayName("Worker: Falla envío a Rabbit, incrementa intentos y guarda el error")
    void process_FailsOnRabbitError() {
        // Arrange
        when(outboxRepository.findAndLockById(event.getId())).thenReturn(java.util.Optional.of(event));
        doThrow(new RuntimeException("Rabbit Down")).when(rabbitTemplate)
                .convertAndSend(anyString(), anyString(), anyString());

        // Act
        outboxWorker.process(event.getId());

        // Assert
        assertFalse(event.isProcessed());
        assertEquals(1, event.getAttempts());
        assertEquals("Rabbit Down", event.getLastError());
        verify(outboxRepository).save(event);
    }

    @Test
    @DisplayName("Worker: Si la fila ya está bloqueada (SKIP LOCKED), no hace nada")
    void process_SkipsIfLocked() {
        // Arrange
        when(outboxRepository.findAndLockById(event.getId())).thenReturn(java.util.Optional.empty());

        // Act
        outboxWorker.process(event.getId());

        // Assert
        verifyNoInteractions(rabbitTemplate);
        verify(outboxRepository, never()).save(any());
    }

    @Test
    @DisplayName("Worker: si ya está procesado, no reenvía ni sobrescribe")
    void process_SkipsAlreadyProcessedEvent() {
        event.setProcessed(true);
        when(outboxRepository.findAndLockById(event.getId())).thenReturn(java.util.Optional.of(event));

        outboxWorker.process(event.getId());

        verifyNoInteractions(rabbitTemplate);
        verify(outboxRepository, never()).save(any());
    }
}
