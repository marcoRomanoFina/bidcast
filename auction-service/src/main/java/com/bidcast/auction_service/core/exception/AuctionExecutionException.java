package com.bidcast.auction_service.core.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Se lanza cuando ocurre un error inesperado durante el proceso de RTB,
 * permitiendo identificar el fallo por sesión para su posterior trazabilidad.
 */
@Getter
public class AuctionExecutionException extends AuctionDomainException {
    private final String sessionId;
    private final String errorCode;

    public AuctionExecutionException(String sessionId, String message, String errorCode, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
        this.sessionId = sessionId;
        this.errorCode = errorCode;
        if (cause != null) {
            this.initCause(cause);
        }
    }
}
