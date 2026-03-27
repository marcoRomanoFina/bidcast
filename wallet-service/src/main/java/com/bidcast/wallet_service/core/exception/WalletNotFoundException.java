package com.bidcast.wallet_service.core.exception;

import java.util.UUID;

public class WalletNotFoundException extends RuntimeException {

    public WalletNotFoundException(UUID ownerId) {
        super("Wallet not found for owner " + ownerId);
    }
}
