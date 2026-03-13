package com.bidcast.auction_service.pop;

import com.bidcast.auction_service.bid.BidInfrastructureService;
import com.bidcast.auction_service.bid.BidPersistenceService;
import com.bidcast.auction_service.bid.BidRehydrationService;
import com.bidcast.auction_service.bid.BidStatus;
import com.bidcast.auction_service.auction.ReceiptTokenService;
import com.bidcast.auction_service.auction.ValidatedReceipt;
import com.bidcast.auction_service.core.exception.InvalidPlayReceiptException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Gestiona el consumo de presupuestos tras la confirmación de reproducción.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProofOfPlayService {

    private final ReceiptTokenService receiptTokenService;
    private final StringRedisTemplate redisTemplate;
    private final ProofOfPlayRepository proofOfPlayRepository;
    private final BidPersistenceService persistenceService;
    private final BidInfrastructureService infrastructureService;
    private final BidRehydrationService rehydrationService;

    private static final long MAX_RECEIPT_AGE_SECONDS = 600;

    /**
     * Procesa una confirmación de reproducción (PoP) de forma funcional y segura.
     */
    public void recordPlay(PopRequest request) {
        log.info("Procesando Proof of Play para la puja {} en la sesión {}", request.bidId(), request.sessionId());

        // 1. Verificación Stateless
        ValidatedReceipt validated = this.validateTicket(request);

        // 2. Control de Idempotencia
        if (this.isAlreadyProcessed(request.playReceiptId())) {
            log.info("Idempotencia: recibo {} ya procesado anteriormente.", request.playReceiptId());
            return;
        }

        // 3. Descuento Atómico en Redis con Auto-Sanación Funcional
        long newBalanceCents = this.decrementBudgetWithFallback(request, validated);

        // 4. Registro Fiscal
        this.persistProofOfPlay(request, validated);

        log.info("PoP registrado exitosamente. Nuevo Saldo: {} centavos", newBalanceCents);

        // 5. Kill-Switch
        long costInCents = validated.advertiserBidPrice().multiply(new BigDecimal("100")).longValue();
        if (newBalanceCents < costInCents) {
            this.handleExhaustedBid(request.sessionId(), request.bidId());
        }
    }

    private ValidatedReceipt validateTicket(PopRequest request) {
        try {
            return receiptTokenService.validateAndExtract(
                    request.playReceiptId(),
                    request.sessionId(),
                    request.getBidIdAsUuid(),
                    MAX_RECEIPT_AGE_SECONDS
            );
        } catch (IllegalArgumentException e) {
            throw new InvalidPlayReceiptException(e.getMessage());
        }
    }

    private boolean isAlreadyProcessed(String receiptId) {
        String key = "pop:processed:" + receiptId;
        return Optional.ofNullable(redisTemplate.opsForValue().setIfAbsent(key, "true", Duration.ofHours(2)))
                .map(isNew -> !isNew)
                .orElse(false);
    }

    private long decrementBudgetWithFallback(PopRequest request, ValidatedReceipt validated) {
        String key = String.format("session:%s:bid:%s:budget", request.sessionId(), request.bidId());
        long cost = validated.advertiserBidPrice().multiply(new BigDecimal("100")).longValue();
        UUID bidUuid = request.getBidIdAsUuid();

        return Optional.ofNullable(redisTemplate.opsForValue().decrement(key, cost))
                .filter(balance -> balance >= 0)
                .orElseGet(() -> {
                    log.warn("Saneando presupuesto para {} tras saldo negativo o ausente.", request.bidId());
                    rehydrationService.rehydrateFullBid(bidUuid);
                    long balance = rehydrationService.calculateRealBalanceCents(bidUuid) - cost;
                    redisTemplate.opsForValue().set(key, String.valueOf(balance));
                    return balance;
                });
    }

    private void persistProofOfPlay(PopRequest request, ValidatedReceipt validated) {
        ProofOfPlay pop = ProofOfPlay.builder()
                .sessionId(request.sessionId())
                .bidId(request.bidId())
                .advertiserId(validated.advertiserId())
                .costCharged(validated.advertiserBidPrice())
                .playReceiptId(request.playReceiptId())
                .playedAt(Instant.now())
                .build();
        proofOfPlayRepository.save(pop);
    }

    public void handleExhaustedBid(String sessionId, String bidId) {
        infrastructureService.removeFromActiveIndex(sessionId, bidId);
        persistenceService.updateStatus(UUID.fromString(bidId), BidStatus.EXHAUSTED);
    }
}
