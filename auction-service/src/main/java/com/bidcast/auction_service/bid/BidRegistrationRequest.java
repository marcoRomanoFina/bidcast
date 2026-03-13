package com.bidcast.auction_service.bid;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record BidRegistrationRequest(
    @NotBlank(message = "El ID de sesión es obligatorio")
    String sessionId,
    
    @NotBlank(message = "El ID del anunciante es obligatorio")
    String advertiserId,
    
    @NotBlank(message = "El ID de la campaña es obligatorio")
    String campaignId,
    
    @NotNull(message = "El presupuesto total es obligatorio")
    @Positive(message = "El presupuesto total debe ser positivo")
    BigDecimal totalBudget,
    
    @NotNull(message = "El precio de puja es obligatorio")
    @Positive(message = "El precio de puja debe ser positivo")
    BigDecimal advertiserBidPrice,
    
    @NotBlank(message = "La URL del anuncio es obligatoria")
    String mediaUrl
) {}
