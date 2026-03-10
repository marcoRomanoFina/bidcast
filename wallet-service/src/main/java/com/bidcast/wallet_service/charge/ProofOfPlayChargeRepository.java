package com.bidcast.wallet_service.charge;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProofOfPlayChargeRepository extends JpaRepository<ProofOfPlayCharge, UUID> {

    Optional<ProofOfPlayCharge> findByProofOfPlayId(UUID proofOfPlayId);
}