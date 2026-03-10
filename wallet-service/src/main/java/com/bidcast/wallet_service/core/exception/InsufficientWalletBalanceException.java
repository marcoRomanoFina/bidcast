package com.bidcast.wallet_service.core.exception;

import java.util.UUID;

public class InsufficientWalletBalanceException extends RuntimeException {

    public InsufficientWalletBalanceException(UUID walletId) {
        super("Saldo insuficiente en wallet " + walletId);
    }
}

