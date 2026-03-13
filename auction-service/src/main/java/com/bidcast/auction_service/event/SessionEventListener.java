package com.bidcast.auction_service.event;

import com.bidcast.auction_service.config.RabbitMQConfig;
import com.bidcast.auction_service.settlement.SettlementOrchestrator;
import com.bidcast.auction_service.session.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SessionEventListener {

    private final SettlementOrchestrator settlementOrchestrator;
    private final SessionService sessionService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_SESSION_STARTED)
    public void handleSessionStarted(@Payload @Valid SessionStartedEvent event) {
        log.info("Recibido evento session.started para la sesión: {}", event.sessionId());
        sessionService.startSession(event.sessionId(), event.deviceId(), event.publisherId());
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_SESSION_CLOSED)
    public void handleSessionClosed(@Payload @Valid SessionClosedEvent event) {
        log.info("Recibido evento session.closed para la sesión: {}", event.sessionId());
        sessionService.closeSession(event.sessionId());
        settlementOrchestrator.orchestrateSettlement(event.sessionId(), event.publisherId());
    }
}
