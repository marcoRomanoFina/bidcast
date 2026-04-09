package com.bidcast.selection_service.settlement;

import com.bidcast.selection_service.offer.OfferInfrastructureService;
import com.bidcast.selection_service.offer.OfferPersistenceService;
import com.bidcast.selection_service.offer.OfferRehydrationService;
import com.bidcast.selection_service.offer.OfferStatus;
import com.bidcast.selection_service.offer.SessionOffer;
import com.bidcast.selection_service.offer.SessionOfferRepository;
import com.bidcast.selection_service.core.event.EventPublisher;
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

    private final OfferPersistenceService persistenceService;
    private final SessionOfferRepository sessionOfferRepository;
    private final OfferInfrastructureService infrastructureService;
    private final OfferRehydrationService rehydrationService;
    private final EventPublisher eventPublisher;

    /**
     * Orquesta el cierre de sesión y la liquidación de fondos.
     */
    @Transactional
    public void orchestrateSettlement(String sessionId, String publisherId) {
        log.info("Starting consolidated settlement for session {}", sessionId);

        List<SessionOffer> offersToSettle = sessionOfferRepository.findBySessionIdAndStatusIn(
                sessionId,
                List.of(OfferStatus.ACTIVE, OfferStatus.EXHAUSTED)
        );
        for (SessionOffer offer : offersToSettle) {
            try {
                BigDecimal spentAmount = calculateSpentAmount(offer);
                if (spentAmount.compareTo(BigDecimal.ZERO) > 0) {
                    // Emitimos el hecho de que la offer ha sido liquidada
                    eventPublisher.publish(new SessionSettledEvent(
                            offer.getId().toString(),
                            offer.getSessionId(),
                            offer.getAdvertiserId(),
                            publisherId,
                            spentAmount,
                            offer.getTotalBudget()
                    ));
                }
                persistenceService.close(offer.getId());
            } catch (Exception e) {
                log.error("Critical failure while settling offer {}: {}", offer.getId(), e.getMessage());
                throw e; 
            }
        }

        // Limpieza de infraestructura caliente
        infrastructureService.purgeSessionIndex(sessionId);
        log.info("Settlement finished for session {}.", sessionId);
    }

    private BigDecimal calculateSpentAmount(SessionOffer offer) {
        log.info("Calculating real spend from DB for offer {}", offer.getId());
        long currentBalanceCents = rehydrationService.calculateRealBalanceCents(offer);

        BigDecimal initialBudget = offer.getTotalBudget();
        BigDecimal remainingBudget = BigDecimal.valueOf(currentBalanceCents)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        return initialBudget.subtract(remainingBudget).setScale(2, RoundingMode.HALF_UP);
    }
}
