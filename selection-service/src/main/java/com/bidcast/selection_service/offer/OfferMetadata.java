package com.bidcast.selection_service.offer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

// Record para guardar la data escencial de una Offer para guardar en redis, asi ahorramos recursos
public record OfferMetadata(
    @NotNull(message = "Offer id is required")
    UUID id,
    
    @NotBlank(message = "Advertiser id is required")
    String advertiserId,
    
    @NotBlank(message = "Campaign id is required")
    String campaignId,
    
    @NotNull(message = "Price per slot is required")
    @Positive(message = "Price per slot must be positive")
    BigDecimal pricePerSlot,

    @NotNull(message = "Device cooldown is required")
    @Positive(message = "Device cooldown must be positive")
    Integer deviceCooldownSeconds,

    @NotNull(message = "Next creative index is required")
    Integer nextCreativeIndex,

    @NotEmpty(message = "At least one creative snapshot is required")
    List<CreativeSnapshot> creatives
) {
    // para pasarlo facil de entidad a metadata
    public static OfferMetadata fromEntity(SessionOffer bid) {
        return new OfferMetadata(
            bid.getId(),
            bid.getAdvertiserId(),
            bid.getCampaignId(),
            bid.getPricePerSlot(),
            bid.getDeviceCooldownSeconds(),
            bid.getNextCreativeIndex(),
            List.copyOf(bid.getCreatives())
        );
    }
}
