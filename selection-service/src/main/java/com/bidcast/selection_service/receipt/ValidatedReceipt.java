package com.bidcast.selection_service.receipt;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

// record para devolver los datos de un PoP validaddo
public record ValidatedReceipt(
    @NotBlank(message = "Advertiser id must not be blank")
    String advertiserId,

    @NotBlank(message = "Creative id must not be blank")
    String creativeId,

    @NotNull(message = "Slot count is required")
    @Positive(message = "Slot count must be positive")
    Integer slotCount,
    
    @NotNull(message = "Slot price is required")
    @Positive(message = "Slot price must be positive")
    BigDecimal pricePerSlot,

    @NotNull(message = "Total price is required")
    @Positive(message = "Total price must be positive")
    BigDecimal totalPrice
) {}
