package com.bidcast.selection_service.selection;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.bidcast.selection_service.core.exception.SelectionInfrastructureUnavailableException;
import com.bidcast.selection_service.offer.CreativeSnapshot;
import com.bidcast.selection_service.offer.OfferInfrastructureService;
import com.bidcast.selection_service.offer.OfferPersistenceService;
import com.bidcast.selection_service.offer.OfferRehydrationService;
import com.bidcast.selection_service.offer.SessionOffer;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SelectionReservationService {

    /**
     * Encapsula los efectos colaterales de una seleccion:
     * consumo del budget caliente, rehydration si falta estado en Redis,
     * cooldown local por device y transicion a EXHAUSTED cuando ya no quedan reproducciones pagables.
     */
    private final OfferInfrastructureService infrastructureService;
    private final OfferRehydrationService rehydrationService;
    private final OfferPersistenceService persistenceService;
    private final SelectionScoringService scoringService;
    private final SelectionPricingService selectionPricingService;

    public long reserveBudgetForSelection(SessionOffer offer, CandidateSelectionRequest request, CreativeSnapshot creative) {
        // Si falta hot state, rehidratamos una sola vez y reintentamos.
        long selectionCostCents = selectionPricingService.selectionCostCents(offer, creative);
        String offerId = offer.getId().toString();

        Optional<Long> newBalance = infrastructureService.decrementBudgetCents(request.sessionId(), offerId, selectionCostCents);
        if (newBalance.isEmpty()) {
            rehydrationService.rehydrateOffer(offer.getId());
            newBalance = infrastructureService.decrementBudgetCents(request.sessionId(), offerId, selectionCostCents);
        }

        if (newBalance.isEmpty()) {
            throw new SelectionInfrastructureUnavailableException("Redis");
        }

        if (newBalance.get() >= 0) {
            if (scoringService.isOfferPermanentlyUnaffordable(offer, newBalance.get())) {
                exhaustOffer(request.sessionId(), offer.getId());
            }
            return newBalance.get();
        }

        Optional<Long> compensated = infrastructureService.incrementBudgetCents(request.sessionId(), offerId, selectionCostCents);
        if (compensated.isEmpty()) {
            throw new SelectionInfrastructureUnavailableException("Redis");
        }

        if (scoringService.isOfferPermanentlyUnaffordable(offer, compensated.get())) {
            exhaustOffer(request.sessionId(), offer.getId());
        }
        return -1L;
    }

    public void reserveCreativeForDevice(SessionOffer offer, CandidateSelectionRequest request, CreativeSnapshot creative) {
        // El cooldown local nace con la seleccion para que dos refills seguidos no reciclen el mismo creative.
        infrastructureService.blockCreativeForDevice(
                request.sessionId(),
                request.deviceId(),
                creative.creativeId(),
                Duration.ofSeconds(offer.getDeviceCooldownSeconds())
        );
    }

    public void exhaustOffer(String sessionId, UUID offerId) {
        infrastructureService.removeFromActiveIndex(sessionId, offerId.toString());
        persistenceService.exhaust(offerId);
    }
}
