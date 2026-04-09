package com.bidcast.selection_service.selection;

import java.math.BigDecimal;

import com.bidcast.selection_service.offer.CreativeSnapshot;
import com.bidcast.selection_service.offer.SessionOffer;

public record SelectionCandidatePick(
        SessionOffer offer,
        CreativeSnapshot creative,
        BigDecimal effectiveScore
) {
    public static SelectionCandidatePick empty(SessionOffer offer) {
        return new SelectionCandidatePick(offer, null, BigDecimal.ZERO);
    }
}
