package com.bidcast.auction_service.settlement;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record SessionSettlementCommand(
    @NotBlank(message = "El ID de la puja es obligatorio")
    String bidId,
    
    @NotBlank(message = "El ID de la sesión es obligatorio")
    String sessionId,
    
    @NotBlank(message = "El ID del anunciante es obligatorio")
    String advertiserId,
    
    @NotBlank(message = "El ID del publisher es obligatorio")
    String publisherId,
    
    @NotNull(message = "El gasto total es obligatorio")
    @Positive(message = "El gasto total debe ser positivo")
    BigDecimal totalSpent,
    
    @NotNull(message = "El presupuesto inicial es obligatorio")
    @Positive(message = "El presupuesto inicial debe ser positivo")
    BigDecimal initialBudget
) {}
