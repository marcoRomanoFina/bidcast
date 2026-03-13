package com.bidcast.auction_service.auction;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * Representa los datos extraídos y validados de un ticket de reproducción.
 */
public record ValidatedReceipt(
    @NotBlank(message = "El ID del anunciante no puede estar vacío")
    String advertiserId, 
    
    @NotNull(message = "El precio de la puja es obligatorio")
    @Positive(message = "El precio de la puja debe ser positivo")
    BigDecimal advertiserBidPrice
) {}
