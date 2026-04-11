package com.bidcast.selection_service.session;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record SelectionSessionRegistrationRequest(
        @NotNull(message = "Session id is required")
        String sessionId,

        @NotNull(message = "Venue id is required")
        UUID venueId,

        @NotNull(message = "Owner id is required")
        UUID ownerId,

        @NotNull(message = "Base price per slot is required")
        BigDecimal basePricePerSlot
) {
}
