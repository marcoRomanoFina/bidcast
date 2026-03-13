package com.bidcast.auction_service.core.exception;

import org.springframework.http.HttpStatus;

public class WalletCommunicationException extends AuctionDomainException {
    public WalletCommunicationException(String message) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
