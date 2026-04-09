package com.bidcast.selection_service.settlement;

import com.bidcast.selection_service.core.event.DomainEvent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Evento que representa la liquidación final de una offer tras el cierre de sesión.
 */
public record SessionSettledEvent(
    UUID eventId,
    Instant occurredOn,
    
    @NotBlank(message = "Offer id is required")
    String offerId,
    
    @NotBlank(message = "Session id is required")
    String sessionId,
    
    @NotBlank(message = "Advertiser id is required")
    String advertiserId,
    
    @NotBlank(message = "Publisher id is required")
    String publisherId,
    
    @NotNull(message = "Total spent is required")
    @Positive(message = "Total spent must be positive")
    BigDecimal totalSpent,
    
    @NotNull(message = "Initial budget is required")
    @Positive(message = "Initial budget must be positive")
    BigDecimal initialBudget
) implements DomainEvent {

    public SessionSettledEvent(String offerId, String sessionId, String advertiserId, String publisherId, BigDecimal totalSpent, BigDecimal initialBudget) {
        this(UUID.randomUUID(), Instant.now(), offerId, sessionId, advertiserId, publisherId, totalSpent, initialBudget);
    }

    @Override
    public String identifier() { return offerId; }
}
