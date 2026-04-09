package com.bidcast.selection_service.offer;

/**
 * Representa el estado consolidado de un Bid tras su rehidratación exitosa.
 * Se utiliza para evitar consultas redundantes a la base de datos.
 */
public record RestoredOffer(
    OfferMetadata metadata,
    long balanceCents
) {}
