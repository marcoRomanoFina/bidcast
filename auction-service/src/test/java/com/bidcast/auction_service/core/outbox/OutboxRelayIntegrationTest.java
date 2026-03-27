package com.bidcast.auction_service.core.outbox;

import com.bidcast.auction_service.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(OutboxRelayIntegrationTest.TestConfig.class)
class OutboxRelayIntegrationTest extends BaseIntegrationTest {

    private final OutboxRelay outboxRelay;
    private final OutboxRepository outboxRepository;
    private final TransactionTemplate transactionTemplate;

    @Autowired
    public OutboxRelayIntegrationTest(OutboxRelay outboxRelay,
                                     OutboxRepository outboxRepository,
                                     TransactionTemplate transactionTemplate) {
        this.outboxRelay = outboxRelay;
        this.outboxRepository = outboxRepository;
        this.transactionTemplate = transactionTemplate;
    }

    // Latch para esperar el mensaje asíncrono de RabbitMQ
    private static CountDownLatch latch = new CountDownLatch(1);
    private static String lastReceivedMessage = null;

    @TestConfiguration
    static class TestConfig {
        @Bean
        public Queue testQueue() { return new Queue("test.queue", false); }

        @Bean
        public TopicExchange testExchange() { return new TopicExchange("test.exchange"); }

        @Bean
        public Binding binding(Queue testQueue, TopicExchange testExchange) {
            return BindingBuilder.bind(testQueue).to(testExchange).with("test.key");
        }

        @RabbitListener(queues = "test.queue")
        public void listen(String message) {
            lastReceivedMessage = message;
            latch.countDown();
        }
    }

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
        latch = new CountDownLatch(1);
        lastReceivedMessage = null;
    }

    @Test
    @DisplayName("Integración 100% Real: DB -> OutboxRelay -> RabbitMQ")
    void scheduleDispatch_FullFlowWithTestcontainers() throws InterruptedException {
        // 1. Guardar evento en Postgres real (desde BaseIntegrationTest)
        String payload = "{\"data\":\"full-integration-message-" + UUID.randomUUID() + "\"}";
        
        UUID eventId = transactionTemplate.execute(status -> {
            OutboxEvent event = new OutboxEvent();
            event.setAggregateId(UUID.randomUUID().toString());
            event.setExchange("test.exchange");
            event.setRoutingKey("test.key");
            event.setPayload(payload);
            event.setProcessed(false);
            return outboxRepository.save(event).getId();
        });

        // 2. Ejecutar el Relay
        outboxRelay.scheduleDispatch();

        // 3. Verificar que el mensaje llegó a RabbitMQ 
        boolean received = latch.await(30, TimeUnit.SECONDS);
        
        // Assertions
        assertTrue(received, "El mensaje debería haber sido recibido por el RabbitListener");
        assertEquals(payload, lastReceivedMessage, "El contenido del mensaje en RabbitMQ debe coincidir con el de la DB");

        // 4. Verificar que se actualizó la DB real tras el envío exitoso
        OutboxEvent updated = outboxRepository.findById(eventId).orElseThrow();
        assertTrue(updated.isProcessed(), "El evento debe estar marcado como procesado en la DB tras enviarse");
        assertNotNull(updated.getProcessedAt());
    }
}
