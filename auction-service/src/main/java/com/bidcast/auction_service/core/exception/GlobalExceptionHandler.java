package com.bidcast.auction_service.core.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuctionDomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(AuctionDomainException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(ErrorResponse.builder()
                        .message(ex.getMessage())
                        .status(ex.getStatus().value())
                        .timestamp(Instant.now())
                        .build());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.builder()
                        .message(ex.getMessage())
                        .status(400)
                        .timestamp(Instant.now())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        return ResponseEntity.internalServerError()
                .body(ErrorResponse.builder()
                        .message("Ha ocurrido un error inesperado")
                        .status(500)
                        .timestamp(Instant.now())
                        .build());
    }
}
