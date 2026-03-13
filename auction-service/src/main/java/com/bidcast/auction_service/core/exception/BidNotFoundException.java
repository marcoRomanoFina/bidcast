package com.bidcast.auction_service.core.exception;

import org.springframework.http.HttpStatus;

public class BidNotFoundException extends AuctionDomainException {
    public BidNotFoundException(String bidId) {
        super("Bid with ID " + bidId + " not found", HttpStatus.NOT_FOUND);
    }
}
