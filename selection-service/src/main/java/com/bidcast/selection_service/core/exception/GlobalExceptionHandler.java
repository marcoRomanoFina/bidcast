package com.bidcast.selection_service.core.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Centraliza el manejo de errores para asegurar que la API siempre responda con
 * un formato estándar, ocultando detalles técnicos sensibles al cliente.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(SelectionDomainException.class)
    public ResponseEntity<ErrorResponse> handleDomainException(SelectionDomainException ex) {
        log.warn("Business exception detected: {} - Status: {}", ex.getMessage(), ex.getStatus());
        return buildResponse(ex.getMessage(), ex.getStatus(), "ERR_DOMAIN_POLICY");
    }

    @ExceptionHandler(SelectionInfrastructureUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleInfrastructureUnavailable(SelectionInfrastructureUnavailableException ex) {
        log.error("Selection infrastructure unavailable: {}", ex.getMessage());
        return buildResponse(ex.getMessage(), HttpStatus.SERVICE_UNAVAILABLE, "ERR_INFRA_UNAVAILABLE");
    }

    @ExceptionHandler(InvalidPlayReceiptException.class)
    public ResponseEntity<ErrorResponse> handleInvalidReceipt(InvalidPlayReceiptException ex) {
        log.warn("Invalid playback receipt detected: {}", ex.getMessage());
        return buildResponse(ex.getMessage(), HttpStatus.BAD_REQUEST, "ERR_INVALID_RECEIPT");
    }

    @ExceptionHandler(InvalidReceiptSignatureException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSignature(InvalidReceiptSignatureException ex) {
        log.error("SECURITY ALERT: Invalid receipt signature for session {}: {}", ex.getSessionId(), ex.getMessage());
        return buildResponse(ex.getMessage(), HttpStatus.BAD_REQUEST, "ERR_SECURITY_SIGNATURE_MISMATCH");
    }

    @ExceptionHandler(OfferNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(OfferNotFoundException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.NOT_FOUND, "ERR_OFFER_NOT_FOUND");
    }

    @ExceptionHandler(WalletCommunicationException.class)
    public ResponseEntity<ErrorResponse> handleWalletError(WalletCommunicationException ex) {
        log.error("Wallet service communication failure: {}", ex.getMessage());
        return buildResponse("Payment service is temporarily unavailable", 
                HttpStatus.SERVICE_UNAVAILABLE, "ERR_WALLET_UNAVAILABLE");
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(jakarta.validation.ConstraintViolationException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.BAD_REQUEST, "ERR_VALIDATION");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return buildResponse(message, HttpStatus.BAD_REQUEST, "ERR_VALIDATION");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return buildResponse("The request body is invalid or malformed",
                HttpStatus.BAD_REQUEST, "ERR_BAD_REQUEST_BODY");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String field = ex.getName() != null ? ex.getName() : "request";
        return buildResponse("Invalid format for '" + field + "'",
                HttpStatus.BAD_REQUEST, "ERR_BAD_REQUEST");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArg(IllegalArgumentException ex) {
        return buildResponse(ex.getMessage(), HttpStatus.BAD_REQUEST, "ERR_BAD_REQUEST");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        log.error("Unhandled internal error: ", ex);
        return buildResponse("An internal error occurred in the service", 
                HttpStatus.INTERNAL_SERVER_ERROR, "ERR_INTERNAL");
    }

    private ResponseEntity<ErrorResponse> buildResponse(String message, HttpStatus status, String code) {
        ErrorResponse error = new ErrorResponse(
                message,
                code,
                status.value(),
                Instant.now()
        );
        return new ResponseEntity<>(error, status);
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }
}
