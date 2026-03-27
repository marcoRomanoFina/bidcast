package com.bidcast.auction_service.bid;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

// Record para guardar la data escencial de un Bid para guardar en redis, asi ahorramos recursos
public record BidMetadata(
    @NotNull(message = "Bid id is required")
    UUID id,
    
    @NotBlank(message = "Advertiser id is required")
    String advertiserId,
    
    @NotBlank(message = "Campaign id is required")
    String campaignId,
    
    @NotNull(message = "Bid price is required")
    @Positive(message = "Bid price must be positive")
    BigDecimal advertiserBidPrice,
    
    @NotBlank(message = "Ad media URL is required")
    String mediaUrl
) {
    // para pasarlo facil de entidad a metadata
    public static BidMetadata fromEntity(SessionBid bid) {
        return new BidMetadata(
            bid.getId(),
            bid.getAdvertiserId(),
            bid.getCampaignId(),
            bid.getAdvertiserBidPrice(),
            bid.getMediaUrl()
        );
    }
}
