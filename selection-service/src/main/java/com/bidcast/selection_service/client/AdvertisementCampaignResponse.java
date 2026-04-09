package com.bidcast.selection_service.client;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

// DTO para encapsular el snapshot que devuelve advertisement-service.
public record AdvertisementCampaignResponse(
        @NotNull(message = "Campaign id is required")
        UUID id,

        @NotBlank(message = "Campaign name is required")
        String name,

        @NotNull(message = "Advertiser id is required")
        UUID advertiserId,

        @NotNull(message = "Campaign budget is required")
        @PositiveOrZero(message = "Campaign budget must be zero or positive")
        BigDecimal budget,

        @NotNull(message = "Campaign spent is required")
        @PositiveOrZero(message = "Campaign spent must be zero or positive")
        BigDecimal spent,

        @NotNull(message = "Campaign bid CPM is required")
        @PositiveOrZero(message = "Campaign bid CPM must be zero or positive")
        BigDecimal bidCpm,

        @NotBlank(message = "Campaign status is required")
        String status,

        @Valid
        @NotEmpty(message = "Campaign must contain at least one creative")
        List<AdvertisementCreativeResponse> creatives
) {
}
