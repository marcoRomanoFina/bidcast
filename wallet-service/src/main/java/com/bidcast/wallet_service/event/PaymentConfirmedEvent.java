package com.bidcast.wallet_service.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Réplica del evento de dominio con mapeo de nombres para compatibilidad.
 */
public record PaymentConfirmedEvent(
    @NotNull UUID eventId,
    @NotNull Instant occurredOn,
    
    @JsonProperty("identifier") // El Billing ahora envía 'identifier'
    @NotNull UUID paymentId, 
    
    @NotNull UUID advertiserId,
    @NotNull @Positive BigDecimal amount
) {}
