package com.bidcast.session_service.core.exception;

import java.util.UUID;

public class SessionDeviceNotFoundException extends RuntimeException {

    public SessionDeviceNotFoundException(UUID sessionId, UUID deviceId) {
        super("Device " + deviceId + " is not part of session " + sessionId);
    }
}
