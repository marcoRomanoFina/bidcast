package com.bidcast.auction_service.bid;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO optimizado para almacenamiento en Redis.
 * Incluye validaciones estrictas para asegurar la integridad de la subasta.
 */
public record BidMetadata(
    @NotNull(message = "El ID del bid es obligatorio")
    UUID id,
    
    @NotBlank(message = "El ID del anunciante es obligatorio")
    String advertiserId,
    
    @NotBlank(message = "El ID de la campaña es obligatorio")
    String campaignId,
    
    @NotNull(message = "El precio de puja es obligatorio")
    @Positive(message = "El precio de puja debe ser positivo")
    BigDecimal advertiserBidPrice,
    
    @NotBlank(message = "La URL del anuncio es obligatoria")
    String mediaUrl
) {
    public static BidMetadata fromEntity(SessionBid bid) {
        return new BidMetadata(
            bid.getId(),
            bid.getAdvertiserId(),
            bid.getCampaignId(),
            bid.getAdvertiserBidPrice(),
            bid.getMediaUrl()
        );
    }
}
