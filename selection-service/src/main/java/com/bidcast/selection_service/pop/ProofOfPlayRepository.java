package com.bidcast.selection_service.pop;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

// queries para los PoP
@Repository
public interface ProofOfPlayRepository extends JpaRepository<ProofOfPlay, UUID> {

    // para no devolver null y evitar un nullpointerEx
    @Query("SELECT COALESCE(SUM(p.costCharged), 0.0) FROM ProofOfPlay p WHERE p.offerId = :offerId")
    BigDecimal sumCostByOfferId(@Param("offerId") String offerId);
}
