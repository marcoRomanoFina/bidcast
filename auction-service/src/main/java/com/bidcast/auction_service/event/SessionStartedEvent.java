package com.bidcast.auction_service.event;

import jakarta.validation.constraints.NotBlank;

// dto para empezar la session
public record SessionStartedEvent(
    @NotBlank(message = "Session ID is required")
    String sessionId,
    
    @NotBlank(message = "Device ID is required")
    String deviceId,
    
    @NotBlank(message = "Publisher ID is required")
    String publisherId
) {}
