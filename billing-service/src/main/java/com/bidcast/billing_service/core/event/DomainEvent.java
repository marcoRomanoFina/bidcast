package com.bidcast.billing_service.core.event;

import java.time.Instant;
import java.util.UUID;

/**
 * Contrato base para todos los eventos de dominio del sistema.
 * El uso de una interfaz permite tratar a cualquier evento de forma polimórfica.
 */
public interface DomainEvent {
    UUID eventId();
    Instant occurredOn();
    String identifier();
}
