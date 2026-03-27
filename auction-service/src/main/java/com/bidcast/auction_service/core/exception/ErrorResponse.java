package com.bidcast.auction_service.core.exception;

import lombok.Builder;
import java.time.Instant;

/**
 * Estructura estándar para respuestas de error de la API.
 */
@Builder
public record ErrorResponse(
    String message,
    String errorCode,
    int status,
    Instant timestamp
) {}
