package com.bidcast.selection_service.core.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Se lanza cuando un Proof of Play llega con una firma HMAC que no coincide,
 * indicando un posible intento de manipulación o spoofing.
 */
@Getter
public class InvalidReceiptSignatureException extends SelectionDomainException {
    private final String sessionId;

    public InvalidReceiptSignatureException(String sessionId, String message) {
        super(message, HttpStatus.BAD_REQUEST);
        this.sessionId = sessionId;
    }
}
