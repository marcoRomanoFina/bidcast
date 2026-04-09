package com.bidcast.selection_service.offer;

import com.bidcast.selection_service.pop.ProofOfPlayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 *  Este service es para la reconstrucción del estado en Redis ante fallos.
 * Actúa como el puente de recuperación entre PostgreSQL y Redis.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OfferRehydrationService {

    private final OfferInfrastructureService infrastructureService;
    private final SessionOfferRepository sessionOfferRepository;
    private final ProofOfPlayRepository popRepository;

    /**
     * Rehidrata una offer individual en Redis.
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
     * Rehidrata todas las offers activas de una sesión.
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
     * Calcula el saldo real basado en Presupuesto Total - Gasto Registrado (PoPs).
     */
    public long calculateRealBalanceCents(SessionOffer offer) {
        // Obtenemos la suma de PoPs desde PostgreSQL (Source of Truth)
        java.math.BigDecimal spent = popRepository.sumCostByOfferId(offer.getId().toString());
        
        // El saldo real es: (Total - Gastado) convertido a centavos para Redis
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
