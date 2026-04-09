package com.bidcast.selection_service.pop;

import com.bidcast.selection_service.offer.OfferInfrastructureService;
import com.bidcast.selection_service.offer.SessionOfferRepository;
import com.bidcast.selection_service.core.exception.InvalidPlayReceiptException;
import com.bidcast.selection_service.core.exception.SelectionInfrastructureUnavailableException;
import com.bidcast.selection_service.receipt.ReceiptTokenService;
import com.bidcast.selection_service.receipt.ValidatedReceipt;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.redisson.client.RedisException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
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

    /**
     * Registra reproducciones efectivamente mostradas.
     * A esta altura la seleccion ya consumio budget; el PoP confirma la evidencia real,
     * resuelve idempotencia y actualiza la recencia global usada por el ranking.
     */
    private final ReceiptTokenService receiptTokenService;
    private final StringRedisTemplate redisTemplate;
    private final ProofOfPlayRepository proofOfPlayRepository;
    private final OfferInfrastructureService infrastructureService;
    private final SessionOfferRepository sessionOfferRepository;

    private static final long MAX_RECEIPT_AGE_SECONDS = 600;
    private static final Duration CAMPAIGN_RECENCY_TTL = Duration.ofMinutes(15);

    /**
     * Procesa una confirmación de reproducción (PoP).
     *
     * Orden importante:
     * 1. validar receipt
     * 2. resolver idempotencia rápida
     * 3. persistir el hecho real en DB
     * 4. marcar idempotencia en Redis
     * 5. actualizar señales operativas como recencia global
     */
    public void recordPlay(PopRequest request) {
        try {
            log.info("Processing Proof of Play for offer {} in session {}", request.offerId(), request.sessionId());

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

            // 4. marcamos el recibo como procesado en Redis.
            markAsProcessed(request.playReceiptId());

            // 5. Extender TTL 
            infrastructureService.extendTTL(request.sessionId(), List.of(request.offerId()));
            registerCampaignPlayback(request.sessionId(), request.getOfferIdAsUuid());

            log.info("PoP recorded successfully for offer {}", request.offerId());
        } catch (SelectionInfrastructureUnavailableException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            if (isRedisUnavailable(ex)) {
                throw new SelectionInfrastructureUnavailableException("Redis");
            }
            throw ex;
        }
    }

    // Valida que el request realmente corresponda a un receipt emitido por selection-service.
    private ValidatedReceipt validateTicket(PopRequest request) {
        try {
            return receiptTokenService.validateAndExtract(
                    request.playReceiptId(),
                    request.sessionId(),
                    request.getOfferIdAsUuid(),
                    request.creativeId(),
                    MAX_RECEIPT_AGE_SECONDS
            );
        } catch (Exception e) {
            throw new InvalidPlayReceiptException(e.getMessage());
        }
    }

    // Idempotencia rápida en Redis para evitar doble procesamiento inmediato.
    private boolean isAlreadyProcessed(String receiptId) {
        String key = "pop:processed:" + receiptId;
        return Optional.ofNullable(redisTemplate.opsForValue().setIfAbsent(key, "processing", Duration.ofHours(2)))
                .map(isNew -> !isNew)
                .orElse(false);
    }

    // Marca el receipt como ya procesado para futuras repeticiones del mismo PoP.
    private void markAsProcessed(String receiptId) {
        String key = "pop:processed:" + receiptId;
        redisTemplate.opsForValue().set(key, "processed", Duration.ofHours(2));
    }

    // Persistencia durable del hecho real de reproducción.
    private boolean persistProofOfPlayIfAbsent(PopRequest request, ValidatedReceipt validated) {
        ProofOfPlay pop = ProofOfPlay.builder()
                .sessionId(request.sessionId())
                .offerId(request.offerId())
                .advertiserId(validated.advertiserId())
                .playReceiptId(request.playReceiptId())
                .costCharged(validated.totalPrice())
                .build();
        try {
            proofOfPlayRepository.save(pop);
            return true;
        } catch (DataIntegrityViolationException ex) {
            log.info("Persistent idempotency: receipt {} already existed in the database.", request.playReceiptId());
            return false;
        }
    }

    // Registra la última reproducción observada de una campaign para ajustar el scoring global.
    private void registerCampaignPlayback(String sessionId, UUID offerId) {
        sessionOfferRepository.findById(offerId).ifPresent(offer -> {
            String key = String.format("session:%s:campaign:%s:last_played", sessionId, offer.getCampaignId());
            redisTemplate.opsForValue().set(key, String.valueOf(Instant.now().toEpochMilli()), CAMPAIGN_RECENCY_TTL);
        });
    }

    // Traduce distintos tipos de fallos Redis/Redisson a una señal común de infraestructura.
    private boolean isRedisUnavailable(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof RedisConnectionFailureException
                    || current instanceof RedisSystemException
                    || current instanceof RedisException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
