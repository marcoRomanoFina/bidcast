package com.bidcast.selection_service.core.exception;

import org.springframework.http.HttpStatus;

public class SelectionInfrastructureUnavailableException extends SelectionDomainException {

    public SelectionInfrastructureUnavailableException(String dependency) {
        super(dependency + " is temporarily unavailable", HttpStatus.SERVICE_UNAVAILABLE);
    }
}
