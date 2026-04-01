package com.bidcast.wallet_service.event;

import com.bidcast.wallet_service.config.RabbitMQConfig;
import com.bidcast.wallet_service.wallet.WalletService;

import jakarta.validation.Valid;

import com.bidcast.wallet_service.wallet.WalletOwnerType;
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
     * Reacciona al Hecho: Pago Confirmado.
     * Este es el patrón Observer en acción.
     */
    @RabbitListener(queues = RabbitMQConfig.QUEUE_CREDIT)
    public void handlePaymentConfirmed(@Valid @Payload PaymentConfirmedEvent event) {
        log.info("Payment confirmed event received for advertiser {}: amount {} - Ref: {}", 
                event.advertiserId(), event.amount(), event.paymentId());
        
        try {
            // El wallet service procesa el crédito usando el paymentId como clave de idempotencia
            walletService.credit(
                event.advertiserId(), 
                WalletOwnerType.ADVERTISER, 
                event.amount(), 
                event.paymentId(),
                "PAYMENT_TOPUP"
            );
            
            log.info("Balance updated successfully for advertiser {}", event.advertiserId());
        } catch (Exception e) {
            log.error("Critical error while processing PaymentConfirmedEvent for {}: {}", 
                    event.advertiserId(), e.getMessage());
            // Lanzamos la excepción para que RabbitMQ pueda gestionar el reintento (DLQ)
            throw e;
        }
    }
}
