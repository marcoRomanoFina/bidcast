package com.bidcast.selection_service.selection;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.bidcast.selection_service.offer.CreativeSnapshot;
import com.bidcast.selection_service.offer.SessionOffer;

@Service
public class SelectionPricingService {

    public long selectionCostCents(SessionOffer offer, CreativeSnapshot creative) {
        return offer.getPricePerSlot()
                .multiply(BigDecimal.valueOf(creative.slotCount()))
                .multiply(new BigDecimal("100"))
                .longValue();
    }
}
