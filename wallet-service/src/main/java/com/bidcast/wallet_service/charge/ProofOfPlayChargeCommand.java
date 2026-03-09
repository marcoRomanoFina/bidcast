package com.bidcast.wallet_service.charge;

import java.math.BigDecimal;
import java.util.UUID;

public record ProofOfPlayChargeCommand(
        UUID proofOfPlayId,
        UUID advertiserWalletId,
        UUID publisherWalletId,
        UUID platformWalletId,
        BigDecimal grossAmount,         
        BigDecimal publisherAmount,     
        BigDecimal platformFeeAmount    
) {
}