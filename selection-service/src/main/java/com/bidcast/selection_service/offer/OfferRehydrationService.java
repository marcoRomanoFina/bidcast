package com.bidcast.selection_service.offer;

import com.bidcast.selection_service.pop.ProofOfPlayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Este service reconstruye el hot state cuando Redis pierde información o una key
 * necesaria ya no está disponible.
 *
 * Conceptualmente actúa como puente entre:
 * - PostgreSQL como source of truth
 * - Redis como estado operativo rápido
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OfferRehydrationService {

    private final OfferInfrastructureService infrastructureService;
    private final SessionOfferRepository sessionOfferRepository;
    private final ProofOfPlayRepository popRepository;

    /**
     * Rehidrata una sola offer.
     *
     * Se usa cuando el hot path necesita una offer puntual y su estado caliente
     * no está disponible.
     */
    public RestoredOffer rehydrateOffer(UUID offerId) {
        return sessionOfferRepository.findById(offerId)
                .map(offer -> {
                    log.warn("Saneando infraestructura Redis para offer: {}", offerId);
                    long balanceCents = calculateRealBalanceCents(offer);
                    infrastructureService.injectIntoRedis(offer, balanceCents);
                    return new RestoredOffer(OfferMetadata.fromEntity(offer), balanceCents);
                })
                .orElseThrow(() -> new RuntimeException("Offer not found for rehydration: " + offerId));
    }

    /**
     * Rehidrata en bloque todas las offers activas de una session.
     *
     * Es útil cuando vuelve Redis o cuando se quiere recomponer todo el hot state
     * de una session activa.
     */
    public void rehydrateSession(String sessionId) {
        log.info("Starting bulk rehydration for session {}", sessionId);
        sessionOfferRepository.findBySessionIdAndStatus(sessionId, OfferStatus.ACTIVE)
                .forEach(offer -> {
                    long balanceCents = calculateRealBalanceCents(offer);
                    infrastructureService.injectIntoRedis(offer, balanceCents);
                });
    }

    /**
     * Calcula el saldo real de una offer según lo persistido.
     *
     * Fórmula:
     * totalBudget - suma(costCharged de todos los PoP)
     */
    public long calculateRealBalanceCents(SessionOffer offer) {
        // PostgreSQL es la fuente real de verdad para el gasto confirmado.
        java.math.BigDecimal spent = popRepository.sumCostByOfferId(offer.getId().toString());
        
        // Redis trabaja con centavos para simplificar operaciones numéricas rápidas.
        return offer.getTotalBudget()
                .subtract(spent)
                .multiply(new java.math.BigDecimal("100"))
                .longValue();
    }

    public long calculateRealBalanceCents(UUID offerId) {
        return sessionOfferRepository.findById(offerId)
                .map(this::calculateRealBalanceCents)
                .orElseThrow(() -> new RuntimeException("Offer not found for balance calculation: " + offerId));
    }
}
