package com.bidcast.auction_service.bid;

import com.bidcast.auction_service.pop.ProofOfPlayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

/**
 * RESPONSABILIDAD: Orquestación de la Sanación y Recuperación de Datos.
 * Coordina entre la persistencia (Postgres) y la infraestructura (Redis).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BidRehydrationService {

    private final BidPersistenceService persistenceService;
    private final BidInfrastructureService infrastructureService;
    private final ProofOfPlayRepository proofOfPlayRepository;

    /**
     * Reconstruye el estado de un bid individual en Redis.
     * Devuelve el objeto restaurado completo para evitar consultas extra.
     */
    public RestoredBid rehydrateFullBid(UUID bidId) {
        log.warn("Saneando infraestructura Redis para bid: {}", bidId);
        
        return persistenceService.findById(bidId)
                .map(bid -> {
                    long balance = this.calculateRealBalanceCents(bid);
                    infrastructureService.injectIntoRedis(bid, balance);
                    return new RestoredBid(BidMetadata.fromEntity(bid), balance);
                })
                .orElseThrow(() -> new RuntimeException("Imposible rehidratar: Bid no encontrado " + bidId));
    }

    /**
     * Reconstruye todos los bids activos de una sesión en Redis.
     */
    public void rehydrateSession(String sessionId) {
        log.warn("Disparando rehidratación masiva para sesión: {}", sessionId);
        
        persistenceService.findActiveBySession(sessionId)
                .forEach(bid -> {
                    long balance = this.calculateRealBalanceCents(bid);
                    infrastructureService.injectIntoRedis(bid, balance);
                });
    }

    /**
     * Consulta Postgres para obtener el saldo remanente real.
     */
    public long calculateRealBalanceCents(SessionBid bid) {
        BigDecimal spent = Optional.ofNullable(proofOfPlayRepository.sumCostByBidId(bid.getId().toString()))
                .orElse(BigDecimal.ZERO);
        BigDecimal remaining = bid.getTotalBudget().subtract(spent).max(BigDecimal.ZERO);
        return remaining.multiply(new BigDecimal("100")).longValue();
    }

    /**
     * Sobrecarga para obtener balance solo por UUID.
     */
    public long calculateRealBalanceCents(UUID bidId) {
        return persistenceService.findById(bidId)
                .map(this::calculateRealBalanceCents)
                .orElseThrow(() -> new RuntimeException("Bid no encontrado para cálculo de saldo: " + bidId));
    }
}
