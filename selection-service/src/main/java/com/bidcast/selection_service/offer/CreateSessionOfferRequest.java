package com.bidcast.selection_service.offer;

import java.math.BigDecimal;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// DTO de entrada para registrar una SessionOffer.
// Representa el momento en que una campaign entra a competir dentro de una session
// con un presupuesto reservado y reglas de reproduccion propias(por ahora solo frequencia/device).
@Schema(description = "Request to register a priced offer for a campaign inside a session")
public record CreateSessionOfferRequest(
        @Schema(description = "Session where the offer will compete", example = "session-1")
        @NotBlank(message = "Session id is required")
        String sessionId,

        @Schema(description = "Advertiser that owns the offer", example = "advertiser-1")
        @NotBlank(message = "Advertiser id is required")
        String advertiserId,

        @Schema(description = "Campaign from advertisement-service that provides the creatives")
        @NotNull(message = "Campaign id is required")
        UUID campaignId,

        @Schema(description = "Total budget reserved for the offer", example = "100.00")
        @NotNull(message = "Total budget is required")
        @Positive(message = "Total budget must be positive")
        BigDecimal totalBudget,

        @Schema(description = "Base price per 5-second slot", example = "0.50")
        @NotNull(message = "Price per slot is required")
        @Positive(message = "Price per slot must be positive")
        BigDecimal pricePerSlot,

        @Schema(description = "Per-device cooldown for the same creative, in seconds", example = "300")
        @NotNull(message = "Device cooldown is required")
        @Positive(message = "Device cooldown must be positive")
        Integer deviceCooldownSeconds
) {
}
