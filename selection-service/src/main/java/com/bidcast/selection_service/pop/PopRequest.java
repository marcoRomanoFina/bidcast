package com.bidcast.selection_service.pop;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

@Schema(description = "Confirmacion de que una reproduccion ya reservada fue efectivamente mostrada")
public record PopRequest(
    @Schema(description = "Session donde ocurrio la reproduccion", example = "session-1")
    @NotBlank(message = "Session id is required")
    String sessionId,

    @Schema(description = "Device que mostro la reproduccion", example = "device-1")
    @NotBlank(message = "Device id is required")
    String deviceId,
    
    @Schema(description = "Offer usada para la reproduccion")
    @NotBlank(message = "Offer id is required")
    String offerId,

    @Schema(description = "Creative que se reprodujo")
    @NotBlank(message = "Creative id is required")
    String creativeId,
    
    @Schema(description = "Receipt firmado entregado previamente por selection")
    @NotBlank(message = "Playback receipt id is required")
    String playReceiptId
) {
    public UUID getOfferIdAsUuid() {
        return UUID.fromString(offerId);
    }
}
