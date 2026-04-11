package com.bidcast.session_service.core.exception;

public class SessionNotificationException extends RuntimeException {

    public SessionNotificationException(String message) {
        super(message);
    }

    public SessionNotificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
