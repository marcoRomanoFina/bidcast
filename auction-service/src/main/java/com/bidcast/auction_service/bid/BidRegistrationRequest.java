package com.bidcast.auction_service.bid;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

// DTO para el registro de un Bid
public record BidRegistrationRequest(
    @NotBlank(message = "Session id is required")
    String sessionId,
    
    @NotBlank(message = "Advertiser id is required")
    String advertiserId,
    
    @NotBlank(message = "Campaign id is required")
    String campaignId,
    
    @NotNull(message = "Total budget is required")
    @Positive(message = "Total budget must be positive")
    BigDecimal totalBudget,
    
    @NotNull(message = "Bid price is required")
    @Positive(message = "Bid price must be positive")
    BigDecimal advertiserBidPrice,
    
    @NotBlank(message = "Ad media URL is required")
    String mediaUrl
) {}
