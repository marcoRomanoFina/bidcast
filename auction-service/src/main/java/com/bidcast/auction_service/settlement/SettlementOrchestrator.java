package com.bidcast.auction_service.settlement;

import com.bidcast.auction_service.bid.BidInfrastructureService;
import com.bidcast.auction_service.bid.BidPersistenceService;
import com.bidcast.auction_service.bid.BidRehydrationService;
import com.bidcast.auction_service.bid.BidStatus;
import com.bidcast.auction_service.bid.SessionBid;
import com.bidcast.auction_service.bid.SessionBidRepository;
import com.bidcast.auction_service.core.outbox.OutboxEvent;
import com.bidcast.auction_service.core.outbox.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * RESPONSABILIDAD: Liquidación final de fondos tras el cierre de sesión.
 * Consolida el gasto registrado en Redis y lo persiste vía Transactional
 * Outbox.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SettlementOrchestrator {

    private final BidPersistenceService persistenceService;
    private final SessionBidRepository sessionBidRepository;
    private final BidInfrastructureService infrastructureService;
    private final BidRehydrationService rehydrationService;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * Orquesta el cierre de sesión y la liquidación de fondos.
     * Implementa el patrón Transactional Outbox para garantizar consistencia
     * eventual.
     */
    @Transactional
    public void orchestrateSettlement(String sessionId, String publisherId) {
        log.info("Starting consolidated settlement (Transactional Outbox) for session {}", sessionId);

        List<SessionBid> activeBids = sessionBidRepository.findBySessionIdAndStatus(sessionId, BidStatus.ACTIVE);
        for (SessionBid bid : activeBids) {
            try {
                // El gasto real se calcula comparando el presupuesto inicial vs lo que queda en
                // Redis.
                BigDecimal spentAmount = calculateSpentAmount(bid);
                if (spentAmount.compareTo(BigDecimal.ZERO) > 0) {
                    // guardamos el event dentro de la transaccion para desp mandarlo
                    saveSettlementCommandInOutbox(bid, publisherId, spentAmount);
                }
                persistenceService.close(bid.getId());
            } catch (Exception e) {
                log.error("Critical failure while settling bid {}: {}", bid.getId(), e.getMessage());
                throw e; // Relanzamos para disparar rollback de la transacción de DB.
            }
        }

        // Limpieza de infraestructura caliente
        infrastructureService.purgeSessionIndex(sessionId);
        log.info("Settlement finished for session {}.", sessionId);
    }

    /**
     * Persiste el comando de cobro en la tabla local.
     * El OutboxRelay se encargará del despacho asíncrono.
     */
    private void saveSettlementCommandInOutbox(SessionBid bid, String pubId, BigDecimal spent) {
        SessionSettlementCommand command = new SessionSettlementCommand(
                bid.getId().toString(),
                bid.getSessionId(),
                bid.getAdvertiserId(),
                pubId,
                spent,
                bid.getTotalBudget());

        try {
            String payload = objectMapper.writeValueAsString(command);
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateId(bid.getId().toString())
                    .exchange("wallet.exchange")
                    .routingKey("wallet.settlement.command")
                    .payload(payload)
                    .build();

            outboxRepository.save(event);
            log.info("Settlement command stored in Outbox for bid {}", bid.getId());
        } catch (DataIntegrityViolationException ex) {
            // Dos cierres concurrentes no deben duplicar el settlement del mismo bid.
            log.info("Settlement for bid {} was already persisted in Outbox. Skipping duplicate.", bid.getId());
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize settlement command", e);
        }
    }

    /**
     * Calcula el gasto real sumando los ProofOfPlay (Audit Trail) de la base de
     * datos.
     * Ignoramos el contador de Redis en el cierre para garantizar consistencia
     * financiera
     * absoluta, incluso si hubo fallos en la infraestructura de caché.
     */
    private BigDecimal calculateSpentAmount(SessionBid bid) {
        log.info("Calculating real spend from DB for bid {}", bid.getId());

        // El BidRehydrationService ya tiene la lógica de sumar PoPs en la DB.
        // Saldo Real (en centavos) = Total - Gastado
        long currentBalanceCents = rehydrationService.calculateRealBalanceCents(bid);

        BigDecimal initialBudget = bid.getTotalBudget();
        BigDecimal remainingBudget = BigDecimal.valueOf(currentBalanceCents)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        // Gasto = Presupuesto Inicial - Saldo que calculamos desde los recibos
        return initialBudget.subtract(remainingBudget).setScale(2, RoundingMode.HALF_UP);
    }
}
