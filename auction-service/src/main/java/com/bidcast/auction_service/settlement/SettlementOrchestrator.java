package com.bidcast.auction_service.settlement;

import com.bidcast.auction_service.bid.BidInfrastructureService;
import com.bidcast.auction_service.bid.BidPersistenceService;
import com.bidcast.auction_service.bid.BidRehydrationService;
import com.bidcast.auction_service.bid.BidStatus;
import com.bidcast.auction_service.bid.SessionBid;
import com.bidcast.auction_service.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * RECONCILIADOR: Cierre de Caja y Liquidación.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementOrchestrator {

    private final BidPersistenceService persistenceService;
    private final BidInfrastructureService infrastructureService;
    private final BidRehydrationService rehydrationService;
    private final StringRedisTemplate redisTemplate;
    private final RabbitTemplate rabbitTemplate;

    public void orchestrateSettlement(String sessionId, String publisherId) {
        log.info("Iniciando liquidación consolidada para sesión: {}", sessionId);

        List<SessionBid> bidsToSettle = persistenceService.findActiveBySession(sessionId);

        if (bidsToSettle.isEmpty()) {
            log.info("No hay transacciones pendientes para liquidar en la sesión {}", sessionId);
            return;
        }

        bidsToSettle.forEach(bid -> this.processBidSettlement(bid, publisherId));

        infrastructureService.purgeSessionIndex(sessionId);
        log.info("Liquidación terminada para sesión {}.", sessionId);
    }

    private void processBidSettlement(SessionBid bid, String publisherId) {
        String budgetKey = String.format("session:%s:bid:%s:budget", bid.getSessionId(), bid.getId());
        String bidId = bid.getId().toString();
        
        try {
            // 1. Calculamos el gasto basándonos en el saldo remanente
            BigDecimal remainingBudget = Optional.ofNullable(redisTemplate.opsForValue().get(budgetKey))
                    .map(centsStr -> {
                        long remainingCents = Long.parseLong(centsStr);
                        return BigDecimal.valueOf(remainingCents)
                                .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
                    })
                    .orElseGet(() -> {
                        // FALLBACK: Si Redis falló, usamos la lógica centralizada de rehidratación
                        log.warn("Presupuesto ausente en Redis para {}. Reconstruyendo desde DB...", bidId);
                        long realRemainingCents = rehydrationService.calculateRealBalanceCents(bid);
                        return BigDecimal.valueOf(realRemainingCents)
                                .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
                    });

            BigDecimal spentAmount = bid.getTotalBudget().subtract(remainingBudget).max(BigDecimal.ZERO);

            // 2. Si hubo gasto, emitimos la orden de cobro final
            if (spentAmount.compareTo(BigDecimal.ZERO) > 0) {
                SessionSettlementCommand command = new SessionSettlementCommand(
                        bidId,
                        bid.getSessionId(),
                        bid.getAdvertiserId(),
                        publisherId,
                        spentAmount,
                        bid.getTotalBudget()
                );
                
                rabbitTemplate.convertAndSend(
                        RabbitMQConfig.EXCHANGE_AUCTION, 
                        RabbitMQConfig.ROUTING_KEY_SETTLEMENT, 
                        command
                );
            }

            persistenceService.updateStatus(bid.getId(), BidStatus.CLOSED);
            infrastructureService.purgeBid(bid.getSessionId(), bidId);

        } catch (Exception e) {
            log.error("Fallo crítico al liquidar puja {}: {}", bidId, e.getMessage());
        }
    }
}
