package com.bidcast.selection_service.pop;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

@Schema(description = "Confirmation that a previously reserved play was actually shown")
// Request del device player para confirmar una reproducción real.
// No crea una selección nueva: solo confirma una que ya fue emitida por selection-service.
public record PopRequest(
    @Schema(description = "Session where the play happened", example = "session-1")
    @NotBlank(message = "Session id is required")
    String sessionId,

    @Schema(description = "Device that rendered the play", example = "device-1")
    @NotBlank(message = "Device id is required")
    String deviceId,
    
    @Schema(description = "Offer used for the play")
    @NotBlank(message = "Offer id is required")
    String offerId,

    @Schema(description = "Creative that was played")
    @NotBlank(message = "Creative id is required")
    String creativeId,
    
    @Schema(description = "Signed receipt previously issued by selection")
    @NotBlank(message = "Playback receipt id is required")
    String playReceiptId
) {
    public UUID getOfferIdAsUuid() {
        return UUID.fromString(offerId);
    }
}
