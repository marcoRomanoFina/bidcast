package com.bidcast.selection_service.selection;

import java.math.BigDecimal;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Reproduccion paga ya confirmada para que el device la ejecute")
public record SelectedCandidate(
        @Schema(description = "Offer que financia la reproduccion")
        UUID offerId,
        @Schema(description = "Session a la que pertenece la reproduccion")
        String sessionId,
        @Schema(description = "Device destino")
        String deviceId,
        @Schema(description = "Advertiser duenio de la offer")
        String advertiserId,
        @Schema(description = "Campaign asociada")
        String campaignId,
        @Schema(description = "Precio base por slot de 5 segundos", example = "0.50")
        BigDecimal pricePerSlot,
        @Schema(description = "Creative concreto que debe reproducirse")
        String creativeId,
        @Schema(description = "URL del media a reproducir")
        String mediaUrl,
        @Schema(description = "Cantidad de slots de 5 segundos que ocupa este creative", example = "3")
        Integer slotCount,
        @Schema(description = "Cooldown local sugerido para este creative en este device, en segundos", example = "300")
        Integer deviceCooldownSeconds,
        @Schema(description = "Receipt firmado que vuelve luego en el PoP")
        String playReceiptId
) {
}
