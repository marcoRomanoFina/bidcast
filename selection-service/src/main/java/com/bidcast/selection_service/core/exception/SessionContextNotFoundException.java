package com.bidcast.selection_service.core.exception;

import org.springframework.http.HttpStatus;

public class SessionContextNotFoundException extends SelectionDomainException {

    public SessionContextNotFoundException(String sessionId) {
        super("Session context not found for session " + sessionId, HttpStatus.NOT_FOUND);
    }
}
