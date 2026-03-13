package com.bidcast.auction_service.bid;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * RESPONSABILIDAD: Persistencia definitiva en PostgreSQL (Cold Data).
 * Maneja el ciclo de vida de las entidades y estados financieros históricos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BidPersistenceService {

    private final SessionBidRepository sessionBidRepository;

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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SessionBid updateStatus(UUID bidId, BidStatus status) {
        return sessionBidRepository.findById(bidId)
                .map(bid -> {
                    bid.setStatus(status);
                    return sessionBidRepository.save(bid);
                })
                .orElseThrow(() -> new RuntimeException("Bid not found: " + bidId));
    }

    public Optional<SessionBid> findById(UUID bidId) {
        return sessionBidRepository.findById(bidId);
    }

    public List<SessionBid> findActiveBySession(String sessionId) {
        return sessionBidRepository.findBySessionIdAndStatusIn(
                sessionId, List.of(BidStatus.ACTIVE)
        );
    }
}
