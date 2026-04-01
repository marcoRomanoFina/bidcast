package com.bidcast.billing_service.payment.event;

import com.bidcast.billing_service.core.event.DomainEvent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Evento de dominio que representa un pago confirmado exitosamente.
 */
public record PaymentConfirmedEvent(
    @NotNull UUID eventId,
    @NotNull Instant occurredOn,
    @NotNull String identifier, 
    @NotNull UUID advertiserId,
    @NotNull @Positive BigDecimal amount
) implements DomainEvent {

    public PaymentConfirmedEvent(UUID paymentId, UUID advertiserId, BigDecimal amount) {
        this(UUID.randomUUID(), Instant.now(), paymentId.toString(), advertiserId, amount);
    }

    public UUID paymentId() {
        return UUID.fromString(identifier);
    }
}
