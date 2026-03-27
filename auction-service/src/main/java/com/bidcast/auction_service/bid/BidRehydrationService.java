package com.bidcast.auction_service.bid;

import com.bidcast.auction_service.pop.ProofOfPlayRepository;
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
public class BidRehydrationService {

    private final BidInfrastructureService infrastructureService;
    private final SessionBidRepository sessionBidRepository;
    private final ProofOfPlayRepository popRepository;

    /**
     * Rehidrata un bid individual en Redis.
     */
    public RestoredBid rehydrateFullBid(UUID bidId) {
        return sessionBidRepository.findById(bidId)
                .map(bid -> {
                    log.warn("Saneando infraestructura Redis para bid: {}", bidId);
                    long balanceCents = calculateRealBalanceCents(bid);
                    infrastructureService.injectIntoRedis(bid, balanceCents);
                    return new RestoredBid(BidMetadata.fromEntity(bid), balanceCents);
                })
                .orElseThrow(() -> new RuntimeException("Bid not found for rehydration: " + bidId));
    }

    /**
     * Rehidrata todos los bids activos de una sesión.
     */
    public void rehydrateSession(String sessionId) {
        log.info("Starting bulk rehydration for session {}", sessionId);
        sessionBidRepository.findBySessionIdAndStatus(sessionId, BidStatus.ACTIVE)
                .forEach(bid -> {
                    long balanceCents = calculateRealBalanceCents(bid);
                    infrastructureService.injectIntoRedis(bid, balanceCents);
                });
    }

    /**
     * Calcula el saldo real basado en Presupuesto Total - Gasto Registrado (PoPs).
     */
    public long calculateRealBalanceCents(SessionBid bid) {
        // Obtenemos la suma de PoPs desde PostgreSQL (Source of Truth)
        java.math.BigDecimal spent = popRepository.sumCostByBidId(bid.getId().toString());
        
        // El saldo real es: (Total - Gastado) convertido a centavos para Redis
        return bid.getTotalBudget()
                .subtract(spent)
                .multiply(new java.math.BigDecimal("100"))
                .longValue();
    }

    public long calculateRealBalanceCents(UUID bidId) {
        return sessionBidRepository.findById(bidId)
                .map(this::calculateRealBalanceCents)
                .orElseThrow(() -> new RuntimeException("Bid not found for balance calculation: " + bidId));
    }
}
