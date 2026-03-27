package com.bidcast.auction_service.auction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

// record para devolver los datos de un PoP validaddo
public record ValidatedReceipt(
    @NotBlank(message = "Advertiser id must not be blank")
    String advertiserId, 
    
    @NotNull(message = "Bid price is required")
    @Positive(message = "Bid price must be positive")
    BigDecimal advertiserBidPrice
) {}
