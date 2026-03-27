package com.bidcast.auction_service.bid;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;


// queries a DB sobre sessionbid
@Repository
public interface SessionBidRepository extends JpaRepository<SessionBid, UUID> {
    List<SessionBid> findBySessionIdAndStatus(String sessionId, BidStatus status);
    List<SessionBid> findBySessionIdAndStatusIn(String sessionId, List<BidStatus> statuses);
}
