package com.bidcast.wallet_service.event;

import com.bidcast.wallet_service.charge.SessionSettlementService;
import com.bidcast.wallet_service.charge.dto.SessionSettlementCommand;
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

    @RabbitListener(queues = RabbitMQConfig.QUEUE_SESSION_SETTLEMENT)
    public void handleSessionSettlement(@Payload @Valid SessionSettlementCommand command) {
        log.info("Settlement command received for bid: {}", command.bidId());
        sessionSettlementService.processSettlement(command);
    }
}
