package com.bidcast.auction_service.core.exception;

import lombok.Builder;
import java.time.Instant;

@Builder
public record ErrorResponse(
    String message,
    int status,
    Instant timestamp
) {}
