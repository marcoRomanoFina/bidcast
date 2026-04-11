package com.bidcast.session_service.session.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DeviceReadyRequest(
        @NotNull(message = "Device id is required")
        UUID deviceId
) {
}
