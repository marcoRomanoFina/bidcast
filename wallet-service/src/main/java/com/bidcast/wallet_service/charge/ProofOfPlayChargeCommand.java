package com.bidcast.wallet_service.charge;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record ProofOfPlayChargeCommand(
    @NotNull UUID proofOfPlayId,
    
    @NotNull 
    @DecimalMin(value = "0.0", inclusive = false, message = "El monto bruto debe ser mayor a cero")
    BigDecimal grossAmount,
    
    @NotNull 
    @DecimalMin(value = "0.0", inclusive = false)
    BigDecimal publisherAmount,
    
    @NotNull 
    @DecimalMin(value = "0.0", inclusive = false)
    BigDecimal platformFeeAmount,
    
    @NotNull UUID advertiserWalletId,
    @NotNull UUID publisherWalletId,
    @NotNull UUID platformWalletId
) {
    // Validación de lógica cruzada (Suma)
    public boolean isSumConsistent() {
        return publisherAmount.add(platformFeeAmount).compareTo(grossAmount) == 0;
    }
}