package com.bidcast.session_service.core.exception;

import java.util.UUID;

public class OpenSessionAlreadyExistsException extends RuntimeException {

    public OpenSessionAlreadyExistsException(UUID venueId) {
        super("Venue " + venueId + " already has an open session");
    }
}
