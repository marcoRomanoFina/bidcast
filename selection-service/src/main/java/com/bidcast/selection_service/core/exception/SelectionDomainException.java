package com.bidcast.selection_service.core.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class SelectionDomainException extends RuntimeException {
    private final HttpStatus status;

    public SelectionDomainException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
}
