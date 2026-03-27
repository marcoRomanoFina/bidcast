package com.bidcast.auction_service.pop;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

// dto para la creacion del PoP
public record PopRequest(
    @NotBlank(message = "Session id is required")
    String sessionId,
    
    @NotBlank(message = "Bid id is required")
    String bidId,
    
    @NotBlank(message = "Playback receipt id is required")
    String playReceiptId
) {
    public UUID getBidIdAsUuid() {
        return UUID.fromString(bidId);
    }
}
