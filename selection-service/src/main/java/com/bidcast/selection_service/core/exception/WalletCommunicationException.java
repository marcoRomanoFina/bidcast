package com.bidcast.selection_service.core.exception;

import org.springframework.http.HttpStatus;

public class WalletCommunicationException extends SelectionDomainException {
    public WalletCommunicationException(String message) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
