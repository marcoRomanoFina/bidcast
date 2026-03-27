package com.bidcast.auction_service.auction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.hibernate.validator.constraints.URL;

import java.util.UUID;

// DTO para mandar al ad ganador al device player
@Builder
public record WinningAd(
    @NotNull(message = "auctionId must not be null")
    UUID auctionId,

    @NotNull(message = "bidId must not be null")
    UUID bidId,

    @NotBlank(message = "mediaUrl is required")
    @URL(message = "mediaUrl must be a valid URL")
    String mediaUrl,

    @NotBlank(message = "advertiserId is required")
    String advertiserId,

    @NotBlank(message = "campaignId is required")
    String campaignId,

    @NotBlank(message = "playReceiptId is required")
    String playReceiptId
) {}
