package com.bidcast.wallet_service.transaction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {
    boolean existsByReferenceIdAndType(UUID referenceId, WalletTransactionType type);
}
