package com.bidcast.auction_service.settlement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

// dto para hacer el settlement de una session

public record SessionSettlementCommand(
    @NotBlank(message = "Bid id is required")
    String bidId,
    
    @NotBlank(message = "Session id is required")
    String sessionId,
    
    @NotBlank(message = "Advertiser id is required")
    String advertiserId,
    
    @NotBlank(message = "Publisher id is required")
    String publisherId,
    
    @NotNull(message = "Total spent is required")
    @Positive(message = "Total spent must be positive")
    BigDecimal totalSpent,
    
    @NotNull(message = "Initial budget is required")
    @Positive(message = "Initial budget must be positive")
    BigDecimal initialBudget
) {}
