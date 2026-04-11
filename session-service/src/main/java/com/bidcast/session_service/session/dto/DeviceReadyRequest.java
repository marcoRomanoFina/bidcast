package com.bidcast.session_service.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record DeviceReadyRequest(
        @Schema(description = "Device that wants to join or activate the session", example = "f7d6c8e4-2ea6-497c-b451-6c9d09d0d198")
        @NotNull(message = "Device id is required")
        UUID deviceId
) {
}
