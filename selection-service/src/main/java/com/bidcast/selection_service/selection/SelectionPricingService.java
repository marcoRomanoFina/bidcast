package com.bidcast.selection_service.selection;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.bidcast.selection_service.offer.CreativeSnapshot;
import com.bidcast.selection_service.offer.SessionOffer;

@Service
// Servicio mínimo de pricing.
// Extrae la fórmula de costo para no duplicarla entre scoring, reserva y PoP.
public class SelectionPricingService {

    // Convierte una reproducción concreta a centavos del hot state.
    public long selectionCostCents(SessionOffer offer, CreativeSnapshot creative) {
        return offer.getPricePerSlot()
                .multiply(BigDecimal.valueOf(creative.slotCount()))
                .multiply(new BigDecimal("100"))
                .longValue();
    }
}
