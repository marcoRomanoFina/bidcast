package com.bidcast.session_service.session.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateSessionRequest(
        @NotNull(message = "Venue id is required")
        UUID venueId,

        @NotBlank(message = "Session name is required")
        String name,

        @NotNull(message = "Owner id is required")
        UUID ownerId,

        @NotNull(message = "Base price per slot is required")
        @Positive(message = "Base price per slot must be positive")
        BigDecimal basePricePerSlot
) {
}
