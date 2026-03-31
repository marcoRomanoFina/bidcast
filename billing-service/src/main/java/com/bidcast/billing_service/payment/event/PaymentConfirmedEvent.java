package com.bidcast.billing_service.payment.event;

import com.bidcast.billing_service.core.event.DomainEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Evento de dominio que representa un pago confirmado exitosamente.
 */
public record PaymentConfirmedEvent(
    UUID eventId,
    Instant occurredOn,
    String aggregateId, 
    UUID advertiserId,
    BigDecimal amount
) implements DomainEvent {

    public PaymentConfirmedEvent(UUID paymentId, UUID advertiserId, BigDecimal amount) {
        this(UUID.randomUUID(), Instant.now(), paymentId.toString(), advertiserId, amount);
    }
    public UUID paymentId(){
        return UUID.fromString(this.aggregateId);
    }
}
