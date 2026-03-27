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

    // metodo para crear una transaccion independiente en el cual se updetea el estado de un sessionBid
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SessionBid updateStatus(UUID bidId, BidStatus status) {
        return sessionBidRepository.findById(bidId)
                .map(bid -> {
                    bid.setStatus(status);
                    return sessionBidRepository.save(bid);
                })
                .orElseThrow(() -> new RuntimeException("Puja no encontrada para actualizar estado: " + bidId));
    }
}
