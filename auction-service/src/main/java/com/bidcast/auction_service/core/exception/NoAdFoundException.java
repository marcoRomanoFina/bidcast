package com.bidcast.auction_service.core.exception;

import lombok.Getter;

@Getter
public class NoAdFoundException extends RuntimeException {
    private final String sessionId;

    public NoAdFoundException(String sessionId) {
        super("No eligible ad was found for session: " + sessionId);
        this.sessionId = sessionId;
    }
}
