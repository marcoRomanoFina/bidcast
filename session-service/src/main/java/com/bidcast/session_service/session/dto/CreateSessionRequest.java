package com.bidcast.session_service.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateSessionRequest(
        @Schema(description = "Venue that owns the runtime session", example = "1b5b27ef-cfbc-4e60-8f04-2ca6f3ab8d90")
        @NotNull(message = "Venue id is required")
        UUID venueId,

        @Schema(description = "Human-friendly session name", example = "Dinner service")
        @NotBlank(message = "Session name is required")
        String name,

        @Schema(description = "Owner of the venue session", example = "f9bc7f34-6e85-4f43-96d5-0e642a64b963")
        @NotNull(message = "Owner id is required")
        UUID ownerId,

        @Schema(description = "Minimum price per slot accepted for this session", example = "3.25")
        @NotNull(message = "Base price per slot is required")
        @Positive(message = "Base price per slot must be positive")
        BigDecimal basePricePerSlot
) {
}
