package com.bidcast.selection_service.offer;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// Snapshot liviano de un creative tomado desde advertisement-service.
// No es el creative "dueño" del dominio publicitario, sino la foto local que
// selection-service necesita para decidir y servir reproducciones sin depender
// de llamadas remotas en el hot path.
@Embeddable
public record CreativeSnapshot(
        @NotBlank(message = "Creative id is required")
        @Column(name = "creative_id", nullable = false)
        String creativeId,

        @NotBlank(message = "Media URL is required")
        @Column(name = "media_url", nullable = false)
        String mediaUrl,

        @NotNull(message = "Slot count is required")
        @Positive(message = "Slot count must be positive")
        @Column(name = "slot_count", nullable = false)
        Integer slotCount
) {
}
