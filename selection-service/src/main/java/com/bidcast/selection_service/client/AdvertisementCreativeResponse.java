package com.bidcast.selection_service.client;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// dto para los creatives embebidos
public record AdvertisementCreativeResponse(
        @NotNull(message = "Creative id is required")
        UUID creativeId,

        @NotBlank(message = "Creative name is required")
        String name,

        @NotBlank(message = "Creative media url is required")
        String mediaUrl,

        @NotBlank(message = "Creative click url is required")
        String clickUrl
) {
}
