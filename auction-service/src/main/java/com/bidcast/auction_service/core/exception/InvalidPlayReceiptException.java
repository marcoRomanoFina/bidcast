package com.bidcast.auction_service.core.exception;

import org.springframework.http.HttpStatus;

public class InvalidPlayReceiptException extends AuctionDomainException {
    public InvalidPlayReceiptException(String reason) {
        super("Invalid play receipt: " + reason, HttpStatus.BAD_REQUEST);
    }
}
