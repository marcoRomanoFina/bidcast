package com.bidcast.selection_service.core.exception;

import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

public class OfferPriceBelowSessionBasePriceException extends SelectionDomainException {

    public OfferPriceBelowSessionBasePriceException(String sessionId, BigDecimal pricePerSlot, BigDecimal basePricePerSlot) {
        super(
                "Offer price per slot " + pricePerSlot + " is below session " + sessionId + " base price " + basePricePerSlot,
                HttpStatus.BAD_REQUEST
        );
    }
}
