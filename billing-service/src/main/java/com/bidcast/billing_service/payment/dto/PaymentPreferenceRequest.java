package com.bidcast.billing_service.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Solicitud para generar un link de pago de recarga.
 */
public record PaymentPreferenceRequest(
    @NotNull(message = "El ID del anunciante es obligatorio")
    UUID advertiserId,

    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser positivo")
    BigDecimal amount,

    String description
) {}
