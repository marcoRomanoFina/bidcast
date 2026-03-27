package com.bidcast.wallet_service.event.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Mensaje de evento para acreditación de fondos (Top-up).
 * Contiene la información necesaria para procesar un pago de forma idempotente.
 */
public record WalletCreditMessage(
    @NotNull(message = "Advertiser ID is required")
    UUID advertiserId,
    
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    BigDecimal amount,
    
    @NotNull(message = "Payment ID is required for idempotency")
    UUID paymentId,
    
    String referenceId
) {}
