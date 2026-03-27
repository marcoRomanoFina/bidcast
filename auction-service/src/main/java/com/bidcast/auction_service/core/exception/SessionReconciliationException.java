package com.bidcast.auction_service.core.exception;

import org.springframework.http.HttpStatus;

/**
 * Lanzada cuando ocurre un error durante el proceso automático de limpieza
 * de sesiones fantasma.
 */
public class SessionReconciliationException extends AuctionDomainException {
    public SessionReconciliationException(String sessionId, String reason) {
        super(String.format("Error reconciling session %s: %s", sessionId, reason), 
              HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
