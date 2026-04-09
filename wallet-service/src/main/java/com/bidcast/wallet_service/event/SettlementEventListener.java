package com.bidcast.wallet_service.event;

import com.bidcast.wallet_service.charge.SessionSettlementService;
import com.bidcast.wallet_service.config.RabbitMQConfig;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SettlementEventListener {

    private final SessionSettlementService sessionSettlementService;

    /**
     * Observador del selection-service.
     * Reacciona cuando una sesión ha sido liquidada y hay que realizar los movimientos financieros.
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_SESSION_SETTLEMENT)
    public void handleSessionSettled(@Payload @Valid SessionSettledEvent event) {
        log.info("Session settled event received for offer: {}. Amount spent: {}", event.offerId(), event.totalSpent());
        sessionSettlementService.processSettlement(event);
    }
}
