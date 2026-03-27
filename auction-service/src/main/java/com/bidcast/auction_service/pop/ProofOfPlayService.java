package com.bidcast.auction_service.pop;

import com.bidcast.auction_service.bid.BidInfrastructureService;
import com.bidcast.auction_service.bid.BidPersistenceService;
import com.bidcast.auction_service.bid.BidRehydrationService;
import com.bidcast.auction_service.bid.BidStatus;
import com.bidcast.auction_service.bid.RestoredBid;
import com.bidcast.auction_service.auction.ReceiptTokenService;
import com.bidcast.auction_service.auction.ValidatedReceipt;
import com.bidcast.auction_service.core.exception.InvalidPlayReceiptException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
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
     * Procesa una confirmación de reproducción (PoP)
     */
    public void recordPlay(PopRequest request) {
        log.info("Processing Proof of Play for bid {} in session {}", request.bidId(), request.sessionId());

        // 1. Verificación Stateless
        ValidatedReceipt validated = this.validateTicket(request);

        // 2. Fast-path de idempotencia en Redis
        if (isAlreadyProcessed(request.playReceiptId())) {
            log.info("Idempotency: receipt {} was already processed previously.", request.playReceiptId());
            return;
        }

        // 3. Persistimos primero en DB para no perder el Source of Truth si Redis falla.
        if (!persistProofOfPlayIfAbsent(request, validated)) {
            markAsProcessed(request.playReceiptId());
            return;
        }

        // 4. Descuento Atómico en Redis
        long newBalanceCents = this.decrementBudgetWithFallback(request, validated);

        // 5. marcamos el recibo como procesado en Redis.
        markAsProcessed(request.playReceiptId());

        // 6. Extender TTL 
        infrastructureService.extendTTL(request.sessionId(), List.of(request.bidId()));

        log.info("PoP recorded successfully. New balance: {} cents", newBalanceCents);

        // 7. si no le alcanza el budget lo marcamos como exhausted
        long costInCents = validated.advertiserBidPrice().multiply(new BigDecimal("100")).longValue();
        if (newBalanceCents < costInCents) {
            handleExhaustedBid(request.sessionId(), request.bidId());
        }
    }

    //metodo privado para validar un PoP
    private ValidatedReceipt validateTicket(PopRequest request) {
        try {
            return receiptTokenService.validateAndExtract(
                    request.playReceiptId(),
                    request.sessionId(),
                    request.getBidIdAsUuid(),
                    MAX_RECEIPT_AGE_SECONDS
            );
        } catch (Exception e) {
            throw new InvalidPlayReceiptException(e.getMessage());
        }
    }

    // metodo privado para chequear si esta procesado un PoP
    private boolean isAlreadyProcessed(String receiptId) {
        String key = "pop:processed:" + receiptId;
        return Optional.ofNullable(redisTemplate.opsForValue().setIfAbsent(key, "processing", Duration.ofHours(2)))
                .map(isNew -> !isNew)
                .orElse(false);
    }

    private void markAsProcessed(String receiptId) {
        String key = "pop:processed:" + receiptId;
        redisTemplate.opsForValue().set(key, "processed", Duration.ofHours(2));
    }


    private long decrementBudgetWithFallback(PopRequest request, ValidatedReceipt validated) {
        String bidKey = String.format("session:%s:bid:%s", request.sessionId(), request.bidId());
        // buscamos cuanto gasto por impresion
        long cost = validated.advertiserBidPrice().multiply(new BigDecimal("100")).longValue();
        UUID bidUuid = request.getBidIdAsUuid();

        // si esta en redis 
        return Optional.ofNullable(redisTemplate.opsForHash().increment(bidKey, "budget", -cost))
                .filter(balance -> balance >= 0)
                .orElseGet(() -> { // sino rehidratamos 
                    log.warn("Repairing budget for {} after negative or missing balance.", request.bidId());
                    RestoredBid restored = rehydrationService.rehydrateFullBid(bidUuid);
                    return restored.balanceCents();
                });
    }

    // metodo privado para guardar el PoP cobrado
    private boolean persistProofOfPlayIfAbsent(PopRequest request, ValidatedReceipt validated) {
        ProofOfPlay pop = ProofOfPlay.builder()
                .sessionId(request.sessionId())
                .bidId(request.bidId())
                .advertiserId(validated.advertiserId())
                .playReceiptId(request.playReceiptId())
                .costCharged(validated.advertiserBidPrice())
                .build();
        try {
            proofOfPlayRepository.save(pop);
            return true;
        } catch (DataIntegrityViolationException ex) {
            log.info("Persistent idempotency: receipt {} already existed in the database.", request.playReceiptId());
            return false;
        }
    }

    //metodo privado para borrar de redis e marcar un Bid EXHAUSTED
    public void handleExhaustedBid(String sessionId, String bidId) {
        infrastructureService.removeFromActiveIndex(sessionId, bidId);
        persistenceService.updateStatus(UUID.fromString(bidId), BidStatus.EXHAUSTED);
    }
}
