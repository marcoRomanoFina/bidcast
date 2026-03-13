package com.bidcast.wallet_service.wallet.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record WalletFreezeRequest(
    @NotBlank(message = "El ID del anunciante es obligatorio")
    String advertiserId,
    
    @NotNull(message = "El monto es obligatorio")
    @Positive(message = "El monto debe ser positivo")
    BigDecimal amount,
    
    @NotBlank(message = "El ID de referencia es obligatorio")
    String referenceId,
    
    String reason
) {}
