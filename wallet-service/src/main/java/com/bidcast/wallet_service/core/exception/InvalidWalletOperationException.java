package com.bidcast.wallet_service.core.exception;

public class InvalidWalletOperationException extends RuntimeException {

    public InvalidWalletOperationException(String message) {
        super(message);
    }
}
