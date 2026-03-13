package com.bidcast.auction_service.core.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class AuctionDomainException extends RuntimeException {
    private final HttpStatus status;

    public AuctionDomainException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}
