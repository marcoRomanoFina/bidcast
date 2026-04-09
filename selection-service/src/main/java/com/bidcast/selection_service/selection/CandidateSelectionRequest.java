package com.bidcast.selection_service.selection;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Pedido del device player para obtener reproducciones pagas ya reservadas")
public record CandidateSelectionRequest(
        @Schema(description = "Identificador de la session de reproduccion", example = "session-1")
        @NotBlank(message = "Session id is required")
        String sessionId,

        @Schema(description = "Identificador del device que va a reproducir", example = "device-1")
        @NotBlank(message = "Device id is required")
        String deviceId,

        @Schema(description = "Cantidad maxima de reproducciones a devolver", example = "2")
        @NotNull(message = "Candidate count is required")
        @Min(value = 1, message = "Candidate count must be at least 1")
        Integer count,

        @Schema(description = "Creatives que el device ya tiene en cola local y no quiere recibir otra vez en este refill")
        List<String> excludedCreativeIds
) {
}
