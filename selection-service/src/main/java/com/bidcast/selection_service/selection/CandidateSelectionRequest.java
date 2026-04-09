package com.bidcast.selection_service.selection;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request sent by the device player to fetch already reserved paid plays")
// Request de entrada del hot path de selección.
// El device no pide "subastar", sino "dame las próximas reproducciones ya resueltas".
public record CandidateSelectionRequest(
        @Schema(description = "Playback session identifier", example = "session-1")
        @NotBlank(message = "Session id is required")
        String sessionId,

        @Schema(description = "Identifier of the device that will play the media", example = "device-1")
        @NotBlank(message = "Device id is required")
        String deviceId,

        @Schema(description = "Maximum number of plays to return", example = "2")
        @NotNull(message = "Candidate count is required")
        @Min(value = 1, message = "Candidate count must be at least 1")
        Integer count,

        @Schema(description = "Creatives already buffered locally by the device and that should not be returned again in this refill")
        List<String> excludedCreativeIds
) {
}
