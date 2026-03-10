package com.bidcast.wallet_service.wallet;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    Optional<Wallet> findByOwnerIdAndOwnerType(UUID ownerId, WalletOwnerType ownerType);
}

