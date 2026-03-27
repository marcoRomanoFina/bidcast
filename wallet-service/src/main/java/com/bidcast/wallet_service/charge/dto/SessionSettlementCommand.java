package com.bidcast.wallet_service.charge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record SessionSettlementCommand(
    @NotBlank(message = "Bid ID is required")
    String bidId,
    
    @NotBlank(message = "Session ID is required")
    String sessionId,
    
    @NotBlank(message = "Advertiser ID is required")
    String advertiserId,
    
    @NotBlank(message = "Publisher ID is required")
    String publisherId,
    
    @NotNull(message = "Total spent is required")
    @Positive(message = "Total spent must be positive")
    BigDecimal totalSpent,
    
    @NotNull(message = "Initial budget is required")
    @Positive(message = "Initial budget must be positive")
    BigDecimal initialBudget
) {}
