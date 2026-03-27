package com.bidcast.wallet_service.event;

import com.bidcast.wallet_service.config.RabbitMQConfig;
import com.bidcast.wallet_service.event.dto.WalletCreditMessage;
import com.bidcast.wallet_service.wallet.WalletService;
import com.bidcast.wallet_service.wallet.WalletOwnerType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WalletCreditEventListener {

    private final WalletService walletService;

    /**
     * Listener para el evento de crédito (Top-up).
     * Aplica validaciones de Bean Validation antes de procesar el mensaje.
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_CREDIT)
    public void handleWalletCredit(@Payload @Valid WalletCreditMessage message) {
        log.info("Credit event received for advertiser {}: amount {} - Ref: {}", 
                message.advertiserId(), message.amount(), message.paymentId());
        
        try {
            // El paymentId es nuestra clave de idempotencia
            walletService.credit(
                message.advertiserId(), 
                WalletOwnerType.ADVERTISER, 
                message.amount(), 
                message.paymentId(),
                "PAYMENT_TOPUP"
            );
            
            log.info("Credit completed successfully for advertiser {}", message.advertiserId());
        } catch (Exception e) {
            log.error("Critical error while processing credit for {}: {}", 
                    message.advertiserId(), e.getMessage());
        }
    }
}
