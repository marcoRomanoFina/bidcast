package com.bidcast.wallet_service.event;

import com.bidcast.wallet_service.config.RabbitMQConfig;
import com.bidcast.wallet_service.wallet.WalletService;
import com.bidcast.wallet_service.wallet.WalletOwnerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class WalletCreditEventListener {

    private final WalletService walletService;

    @RabbitListener(queues = RabbitMQConfig.QUEUE_CREDIT)
    public void handleWalletCredit(WalletCreditMessage message) {
        log.info("Recibido evento de crédito para anunciante {}: monto {}", message.advertiserId(), message.amount());
        
        try {
            walletService.credit(message.advertiserId(), WalletOwnerType.ADVERTISER, message.amount());
            log.info("Crédito realizado exitosamente para anunciante {}", message.advertiserId());
        } catch (Exception e) {
            log.error("Error al procesar crédito de billetera: {}", e.getMessage());
        }
    }

    public record WalletCreditMessage(
        UUID advertiserId,
        BigDecimal amount,
        String paymentId,
        String referenceId
    ) {}
}
