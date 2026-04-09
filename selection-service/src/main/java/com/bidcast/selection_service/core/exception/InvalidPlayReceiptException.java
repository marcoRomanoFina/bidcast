package com.bidcast.selection_service.core.exception;

import org.springframework.http.HttpStatus;

public class InvalidPlayReceiptException extends SelectionDomainException {
    public InvalidPlayReceiptException(String reason) {
        super("Invalid play receipt: " + reason, HttpStatus.BAD_REQUEST);
    }
}
