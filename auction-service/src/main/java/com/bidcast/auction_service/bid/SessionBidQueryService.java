package com.bidcast.auction_service.bid;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SessionBidQueryService {

    private final SessionBidRepository sessionBidRepository;

    @Cacheable(value = "activeBids", key = "#sessionId")
    @Transactional(readOnly = true)
    public List<SessionBid> getActiveBidsForSession(String sessionId) {
        return sessionBidRepository.findBySessionIdAndStatus(sessionId, BidStatus.ACTIVE);
    }
}
