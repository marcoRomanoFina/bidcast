package com.bidcast.selection_service.offer;

import java.math.BigDecimal;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Alta de una offer economica por campaign dentro de una session")
public record CreateSessionOfferRequest(
        @Schema(description = "Session donde la offer va a competir", example = "session-1")
        @NotBlank(message = "Session id is required")
        String sessionId,

        @Schema(description = "Advertiser duenio de la offer", example = "advertiser-1")
        @NotBlank(message = "Advertiser id is required")
        String advertiserId,

        @Schema(description = "Campaign del advertisement-service que aporta creatives")
        @NotNull(message = "Campaign id is required")
        UUID campaignId,

        @Schema(description = "Budget total reservado para la offer", example = "100.00")
        @NotNull(message = "Total budget is required")
        @Positive(message = "Total budget must be positive")
        BigDecimal totalBudget,

        @Schema(description = "Precio base por slot de 5 segundos", example = "0.50")
        @NotNull(message = "Price per slot is required")
        @Positive(message = "Price per slot must be positive")
        BigDecimal pricePerSlot,

        @Schema(description = "Cooldown local por device para un mismo creative, en segundos", example = "300")
        @NotNull(message = "Device cooldown is required")
        @Positive(message = "Device cooldown must be positive")
        Integer deviceCooldownSeconds
) {
}
