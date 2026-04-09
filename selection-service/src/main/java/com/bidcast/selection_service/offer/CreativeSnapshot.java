package com.bidcast.selection_service.offer;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

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
