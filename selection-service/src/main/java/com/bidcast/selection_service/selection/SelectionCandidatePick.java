package com.bidcast.selection_service.selection;

import java.math.BigDecimal;

import com.bidcast.selection_service.offer.CreativeSnapshot;
import com.bidcast.selection_service.offer.SessionOffer;

// Resultado intermedio del scoring.
// Sirve para separar:
// - cuál offer quedó mejor puntuada
// - qué creative concreto se eligió dentro de esa offer
// - con qué score compite contra otras offers
public record SelectionCandidatePick(
        SessionOffer offer,
        CreativeSnapshot creative,
        BigDecimal effectiveScore
) {
    // Conveniencia para representar "esta offer no puede competir ahora".
    public static SelectionCandidatePick empty(SessionOffer offer) {
        return new SelectionCandidatePick(offer, null, BigDecimal.ZERO);
    }
}
