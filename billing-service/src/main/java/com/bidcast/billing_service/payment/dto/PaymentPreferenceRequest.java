package com.bidcast.billing_service.payment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Solicitud para generar un link de pago de recarga.
 */
public record PaymentPreferenceRequest(
    @NotNull(message = "Advertiser ID is required")
    UUID advertiserId,

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    BigDecimal amount,

    String description
) {}
