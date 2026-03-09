package com.bidcast.wallet_service.core.exception;

public class ConcurrentProofOfPlayException extends RuntimeException {
    
    public ConcurrentProofOfPlayException(String message, Throwable cause) {
        super(message, cause);
    }
}