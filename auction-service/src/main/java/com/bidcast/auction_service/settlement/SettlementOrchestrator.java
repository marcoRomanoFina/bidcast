package com.bidcast.auction_service.settlement;

import com.bidcast.auction_service.bid.BidInfrastructureService;
import com.bidcast.auction_service.bid.BidPersistenceService;
import com.bidcast.auction_service.bid.BidRehydrationService;
import com.bidcast.auction_service.bid.BidStatus;
import com.bidcast.auction_service.bid.SessionBid;
import com.bidcast.auction_service.bid.SessionBidRepository;
import com.bidcast.auction_service.core.event.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * RESPONSABILIDAD: Liquidación final de fondos tras el cierre de sesión.
 * Consolida el gasto registrado en Redis y lo publica vía EventPublisher (Outbox).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementOrchestrator {

    private final BidPersistenceService persistenceService;
    private final SessionBidRepository sessionBidRepository;
    private final BidInfrastructureService infrastructureService;
    private final BidRehydrationService rehydrationService;
    private final EventPublisher eventPublisher;

    /**
     * Orquesta el cierre de sesión y la liquidación de fondos.
     */
    @Transactional
    public void orchestrateSettlement(String sessionId, String publisherId) {
        log.info("Starting consolidated settlement for session {}", sessionId);

        List<SessionBid> activeBids = sessionBidRepository.findBySessionIdAndStatus(sessionId, BidStatus.ACTIVE);
        for (SessionBid bid : activeBids) {
            try {
                BigDecimal spentAmount = calculateSpentAmount(bid);
                if (spentAmount.compareTo(BigDecimal.ZERO) > 0) {
                    // Emitimos el Hecho de que la puja ha sido liquidada
                    eventPublisher.publish(new SessionSettledEvent(
                            bid.getId().toString(),
                            bid.getSessionId(),
                            bid.getAdvertiserId(),
                            publisherId,
                            spentAmount,
                            bid.getTotalBudget()
                    ));
                }
                persistenceService.close(bid.getId());
            } catch (Exception e) {
                log.error("Critical failure while settling bid {}: {}", bid.getId(), e.getMessage());
                throw e; 
            }
        }

        // Limpieza de infraestructura caliente
        infrastructureService.purgeSessionIndex(sessionId);
        log.info("Settlement finished for session {}.", sessionId);
    }

    private BigDecimal calculateSpentAmount(SessionBid bid) {
        log.info("Calculating real spend from DB for bid {}", bid.getId());
        long currentBalanceCents = rehydrationService.calculateRealBalanceCents(bid);

        BigDecimal initialBudget = bid.getTotalBudget();
        BigDecimal remainingBudget = BigDecimal.valueOf(currentBalanceCents)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        return initialBudget.subtract(remainingBudget).setScale(2, RoundingMode.HALF_UP);
    }
}
