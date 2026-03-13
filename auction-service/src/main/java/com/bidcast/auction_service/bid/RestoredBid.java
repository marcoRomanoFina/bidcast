package com.bidcast.auction_service.bid;

/**
 * Representa el estado consolidado de un Bid tras su rehidratación exitosa.
 * Se utiliza para evitar consultas redundantes a la base de datos.
 */
public record RestoredBid(
    BidMetadata metadata,
    long balanceCents
) {}
