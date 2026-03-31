package com.bidcast.auction_service.bid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Persistencia definitiva en PostgreSQL (Cold Data).
 * Maneja el ciclo de vida de las entidades y estados financieros históricos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BidPersistenceService {

    private final SessionBidRepository sessionBidRepository;

    // metodo para crear una transaccion independiente para guardar un nuevo request con su estado inicial PENDING
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SessionBid saveAsPending(BidRegistrationRequest request) {
        SessionBid bid = SessionBid.builder()
                .sessionId(request.sessionId())
                .advertiserId(request.advertiserId())
                .campaignId(request.campaignId())
                .totalBudget(request.totalBudget())
                .advertiserBidPrice(request.advertiserBidPrice())
                .mediaUrl(request.mediaUrl())
                .status(BidStatus.PENDING_RESERVATION)
                .build();
        return sessionBidRepository.save(bid);
    }

    // Activa la puja
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SessionBid activate(UUID bidId) {
        return sessionBidRepository.findById(bidId)
                .map(bid -> {
                    bid.activate();
                    return sessionBidRepository.save(bid);
                })
                .orElseThrow(() -> new RuntimeException("Bid not found to activate: " + bidId));
    }

    // Marca la puja como fallida
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void fail(UUID bidId) {
        sessionBidRepository.findById(bidId).ifPresent(bid -> {
            bid.fail();
            sessionBidRepository.save(bid);
        });
    }

    // Marca un fallo crítico
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCriticalFailure(UUID bidId) {
        sessionBidRepository.findById(bidId).ifPresent(bid -> {
            bid.markCriticalFailure();
            sessionBidRepository.save(bid);
        });
    }

    // Marca la puja como agotada (sin presupuesto)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void exhaust(UUID bidId) {
        sessionBidRepository.findById(bidId).ifPresent(bid -> {
            // Necesitamos añadir este método en la entidad SessionBid
            bid.exhaust();
            sessionBidRepository.save(bid);
        });
    }

    // Cierra la puja al finalizar la sesión
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void close(UUID bidId) {
        sessionBidRepository.findById(bidId).ifPresent(bid -> {
            bid.close();
            sessionBidRepository.save(bid);
        });
    }
}
