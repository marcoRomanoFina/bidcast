package com.bidcast.auction_service.pop;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
public interface ProofOfPlayRepository extends JpaRepository<ProofOfPlay, UUID> {

    @Query("SELECT COALESCE(SUM(p.costCharged), 0.0) FROM ProofOfPlay p WHERE p.bidId = :bidId")
    BigDecimal sumCostByBidId(@Param("bidId") String bidId);
}
