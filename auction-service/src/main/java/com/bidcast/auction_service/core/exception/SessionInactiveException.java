package com.bidcast.auction_service.core.exception;

import org.springframework.http.HttpStatus;

/**
 * Excepción lanzada cuando se intenta realizar una operación sobre una sesión
 * que no existe o ya ha sido finalizada.
 */
public class SessionInactiveException extends AuctionDomainException {
    public SessionInactiveException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}
