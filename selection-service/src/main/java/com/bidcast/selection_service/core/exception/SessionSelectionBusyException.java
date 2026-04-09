package com.bidcast.selection_service.core.exception;

import org.springframework.http.HttpStatus;

public class SessionSelectionBusyException extends SelectionDomainException {

    public SessionSelectionBusyException(String sessionId) {
        super("Selection is already running for session " + sessionId, HttpStatus.CONFLICT);
    }
}
