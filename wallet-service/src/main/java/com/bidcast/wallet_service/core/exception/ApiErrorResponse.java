package com.bidcast.wallet_service.core.exception;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ApiErrorResponse", description = "Standard error response for wallet-service operations")
public record ApiErrorResponse(
        @Schema(description = "Business or infrastructure error message", example = "Wallet not found for owner 8c44f4b5-cd54-4fe6-b1a8-f617e9f0f807")
        String error
) {
}
