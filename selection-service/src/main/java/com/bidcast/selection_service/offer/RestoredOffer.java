package com.bidcast.selection_service.offer;

/**
 * Resultado de una rehidratación exitosa.
 *
 * Devuelve juntas las dos piezas que el caller necesita:
 * - metadata reconstruida
 * - balance caliente en centavos
 */
public record RestoredOffer(
    OfferMetadata metadata,
    long balanceCents
) {}
