package com.bidcast.auction_service.core.exception;

import lombok.Getter;

/**
 lanzada cuando se intenta procesar una subasta para una sesión
 que ya tiene una operación en curso en otro worker/hilo.
 */
@Getter
public class SessionConcurrencyException extends RuntimeException {
    private final String sessionId;
    private final String errorCode;

    public SessionConcurrencyException(String sessionId, String message) {
        super(message);
        this.sessionId = sessionId;
        this.errorCode = "ERR_SESSION_CONCURRENCY_LIMIT";
    }
}
