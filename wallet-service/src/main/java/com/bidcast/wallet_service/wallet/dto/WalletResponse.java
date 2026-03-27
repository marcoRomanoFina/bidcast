package com.bidcast.wallet_service.wallet.dto;

import com.bidcast.wallet_service.wallet.Wallet;
import com.bidcast.wallet_service.wallet.WalletOwnerType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WalletResponse(
        UUID id,
        UUID ownerId,
        WalletOwnerType ownerType,
        String currencyCode,
        BigDecimal balance,
        BigDecimal frozenBalance,
        Instant createdAt,
        Instant updatedAt
) {
    public static WalletResponse from(Wallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getOwnerId(),
                wallet.getOwnerType(),
                wallet.getCurrencyCode(),
                wallet.getBalance(),
                wallet.getFrozenBalance(),
                wallet.getCreatedAt(),
                wallet.getUpdatedAt()
        );
    }
}
