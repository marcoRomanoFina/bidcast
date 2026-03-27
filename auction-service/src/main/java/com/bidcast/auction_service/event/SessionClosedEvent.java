package com.bidcast.auction_service.event;

import jakarta.validation.constraints.NotBlank;

//dto para cerrar la session
public record SessionClosedEvent(
    @NotBlank(message = "Session id is required")
    String sessionId,
    
    @NotBlank(message = "Publisher id is required")
    String publisherId
) {}
