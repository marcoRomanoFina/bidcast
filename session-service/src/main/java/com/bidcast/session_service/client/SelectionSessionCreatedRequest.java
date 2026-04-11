package com.bidcast.session_service.client;

import java.math.BigDecimal;
import java.util.UUID;

public record SelectionSessionCreatedRequest(
        String sessionId,
        UUID venueId,
        UUID ownerId,
        BigDecimal basePricePerSlot
) {
}
