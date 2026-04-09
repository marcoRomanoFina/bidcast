package com.bidcast.selection_service.offer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

// queries a DB sobre session offers
@Repository
public interface SessionOfferRepository extends JpaRepository<SessionOffer, UUID> {
    List<SessionOffer> findBySessionIdAndStatus(String sessionId, OfferStatus status);
    List<SessionOffer> findBySessionIdAndStatusIn(String sessionId, List<OfferStatus> statuses);
}
