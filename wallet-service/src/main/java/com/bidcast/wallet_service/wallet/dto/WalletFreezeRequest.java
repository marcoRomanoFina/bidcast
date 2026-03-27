package com.bidcast.wallet_service.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Schema(name = "WalletFreezeRequest", description = "Payload to freeze or unfreeze funds from an advertiser wallet")
public record WalletFreezeRequest(
    @Schema(description = "Authenticated advertiser identifier", example = "2b9fd6d4-ef58-4d56-9aa2-8d6f72d5ce59", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Advertiser ID is required")
    String advertiserId,

    @Schema(description = "Amount to freeze or unfreeze", example = "1500.00", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    BigDecimal amount,

    @Schema(description = "Business reference that identifies the operation", example = "bid-session-91f7d4", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "Reference ID is required")
    String referenceId,

    @Schema(description = "Functional reason for the operation", example = "auction hold")
    String reason
) {}
