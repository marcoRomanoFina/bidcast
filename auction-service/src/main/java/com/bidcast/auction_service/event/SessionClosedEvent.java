package com.bidcast.auction_service.event;

import jakarta.validation.constraints.NotBlank;

public record SessionClosedEvent(
    @NotBlank(message = "El ID de sesión es obligatorio")
    String sessionId,
    
    @NotBlank(message = "El ID del publisher es obligatorio")
    String publisherId
) {}
