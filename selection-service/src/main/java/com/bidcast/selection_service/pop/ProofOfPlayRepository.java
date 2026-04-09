package com.bidcast.selection_service.pop;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

// Repository de PoP.
// Se usa tanto para persistencia auditada como para calcular gasto real en settlement/rehydration.
@Repository
public interface ProofOfPlayRepository extends JpaRepository<ProofOfPlay, UUID> {

    // Devuelve 0 si todavía no hay reproducciones, así el caller no tiene que defender nulls.
    @Query("SELECT COALESCE(SUM(p.costCharged), 0.0) FROM ProofOfPlay p WHERE p.offerId = :offerId")
    BigDecimal sumCostByOfferId(@Param("offerId") String offerId);
}
