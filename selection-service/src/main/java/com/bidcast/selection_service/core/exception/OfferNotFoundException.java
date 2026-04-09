package com.bidcast.selection_service.core.exception;

import org.springframework.http.HttpStatus;

public class OfferNotFoundException extends SelectionDomainException {
    public OfferNotFoundException(String offerId) {
        super("Offer with ID " + offerId + " not found", HttpStatus.NOT_FOUND);
    }
}
