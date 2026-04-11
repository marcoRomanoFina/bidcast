package com.bidcast.selection_service.selection;

import java.time.Duration;
import java.util.Optional;
import java.util.OptionalLong;
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
// Encapsula los efectos laterales de una selección ya elegida.
// La idea es que el scoring solo "decide", y este service "materializa" esa decisión.
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

    /**
     * Consume presupuesto operativo para una reproducción concreta.
     *
     * Flujo:
     * - intenta descontar en Redis
     * - si falta la key, rehidrata y reintenta una vez
     * - si queda saldo negativo, compensa el descuento y devuelve vacío
     * - si la offer ya no puede pagar ningún creative, la agota
     */
    public OptionalLong consumeBudgetForSelection(SessionOffer offer, CandidateSelectionRequest request, CreativeSnapshot creative) {
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

        // Si todavía queda saldo no negativo, la selección quedó consumida correctamente.
        if (newBalance.get() >= 0) {
            if (scoringService.isOfferPermanentlyUnaffordable(offer, newBalance.get())) {
                exhaustOffer(request.sessionId(), offer.getId());
            }
            return OptionalLong.of(newBalance.get());
        }

        // Si cayó por debajo de cero, compensamos el descuento y rechazamos esta selección.
        Optional<Long> compensated = infrastructureService.incrementBudgetCents(request.sessionId(), offerId, selectionCostCents);
        if (compensated.isEmpty()) {
            throw new SelectionInfrastructureUnavailableException("Redis");
        }

        if (scoringService.isOfferPermanentlyUnaffordable(offer, compensated.get())) {
            exhaustOffer(request.sessionId(), offer.getId());
        }
        return OptionalLong.empty();
    }

    /**
     * Aplica el cooldown local por device una vez que la selección ya va a devolverse.
     */
    public void reserveCreativeForDevice(SessionOffer offer, CandidateSelectionRequest request, CreativeSnapshot creative) {
        // El cooldown local nace con la seleccion para que dos refills seguidos no reciclen el mismo creative.
        infrastructureService.blockCreativeForDevice(
                request.sessionId(),
                request.deviceId(),
                creative.creativeId(),
                Duration.ofSeconds(offer.getDeviceCooldownSeconds())
        );
    }

    /**
     * Saca una offer del hot path y la deja marcada como EXHAUSTED en persistencia.
     */
    public void exhaustOffer(String sessionId, UUID offerId) {
        infrastructureService.removeFromActiveIndex(sessionId, offerId.toString());
        persistenceService.exhaust(offerId);
    }
}
