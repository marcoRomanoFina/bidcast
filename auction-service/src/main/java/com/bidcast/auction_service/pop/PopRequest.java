package com.bidcast.auction_service.pop;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record PopRequest(
    @NotBlank(message = "El ID de sesión es obligatorio")
    String sessionId,
    
    @NotBlank(message = "El ID de la puja es obligatorio")
    String bidId,
    
    @NotBlank(message = "El ID del recibo de reproducción es obligatorio")
    String playReceiptId
) {
    public UUID getBidIdAsUuid() {
        return UUID.fromString(bidId);
    }
}
