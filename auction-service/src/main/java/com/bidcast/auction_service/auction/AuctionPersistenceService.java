package com.bidcast.auction_service.auction;

import com.bidcast.auction_service.core.outbox.OutboxEvent;
import com.bidcast.auction_service.core.outbox.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio dedicado a la persistencia del motor de subastas.
 * Aislamos el manejo de transacciones de base de datos de la lógica pesada
 * de red y CPU del AuctionEngine.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionPersistenceService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    /**
     * Persiste el resultado de una subasta exitosa.
     */
    @Transactional
    public void persistAuctionSuccess(WinningAd result) {
        log.debug("Persisting auction audit and Outbox event for id {}", result.auctionId());
        
        try {
            // Generamos el evento para el Outbox
            String payload = objectMapper.writeValueAsString(result);
            
            OutboxEvent event = OutboxEvent.builder()
                    .aggregateId(result.auctionId().toString())
                    .exchange("bidcast.auction") // Exchange de eventos de subasta
                    .routingKey("auction.won")   // Routing key para auditoría y cobros
                    .payload(payload)
                    .processed(false)
                    .attempts(0)
                    .build();

            outboxRepository.save(event);
            
            log.info("Auction {} persisted successfully in Outbox.", result.auctionId());
        } catch (DataIntegrityViolationException ex) {
            // Si el mismo aggregate/routing ya existe, tratamos el evento como ya persistido.
            log.info("Auction event {} already existed in Outbox. Treating as idempotent.", result.auctionId());
            
        } catch (Exception e) {
            log.error("Critical error persisting auction result {}: {}", result.auctionId(), e.getMessage());
            // Relanzamos para que Spring haga Rollback de la transacción
            throw new RuntimeException("Auction persistence failure", e);
        }
    }
}
