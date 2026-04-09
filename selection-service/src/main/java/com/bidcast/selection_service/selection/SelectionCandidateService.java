package com.bidcast.selection_service.selection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bidcast.selection_service.offer.OfferStatus;
import com.bidcast.selection_service.offer.CreativeSnapshot;
import com.bidcast.selection_service.offer.SessionOffer;
import com.bidcast.selection_service.offer.SessionOfferRepository;
import com.bidcast.selection_service.receipt.ReceiptTokenService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
// Orquestador principal del hot path.
// No hace toda la lógica él solo: coordina lock, scoring, reserva y construcción del DTO final.
public class SelectionCandidateService {

    /**
     * Orquesta el hot path de seleccion.
     * Mantiene el flujo principal corto: lock por session, scoring de offers, reserva de budget
     * y construccion de la respuesta que consume el device player.
     */
    private final SessionOfferRepository sessionOfferRepository;
    private final SelectionLockService selectionLockService;
    private final SelectionScoringService selectionScoringService;
    private final SelectionReservationService selectionReservationService;
    private final ReceiptTokenService receiptTokenService;

    @Transactional
    public List<SelectedCandidate> selectCandidates(CandidateSelectionRequest request) {
        return selectionLockService.withSessionLock(request.sessionId(), () -> {
            // 1. Cargamos offers activas de la session.
            List<SessionOffer> activeOffers = sessionOfferRepository.findBySessionIdAndStatus(request.sessionId(), OfferStatus.ACTIVE);
            if (activeOffers.isEmpty()) {
                return List.of();
            }

            // 2. Partimos de exclusiones ya conocidas por el device.
            Set<String> excludedCreatives = new HashSet<>(
                    request.excludedCreativeIds() == null ? List.of() : request.excludedCreativeIds()
            );
            Map<String, Set<String>> blockedCreativesCache = new HashMap<>();
            Map<String, java.math.BigDecimal> campaignPenaltyCache = new HashMap<>();
            Set<SessionOffer> touchedOffers = new HashSet<>();
            List<SelectedCandidate> selected = new ArrayList<>();

            // 3. Intentamos armar hasta N reproducciones ya confirmadas.
            for (int i = 0; i < request.count(); i++) {
                // 3.a. Elegimos la mejor combinación offer + creative disponible.
                Optional<SelectionCandidatePick> best = activeOffers.stream()
                        .map(offer -> selectionScoringService.candidateForOffer(
                                offer,
                                request,
                                excludedCreatives,
                                blockedCreativesCache,
                                campaignPenaltyCache
                        ))
                        .filter(pick -> pick.creative() != null)
                        .max(Comparator.comparing(SelectionCandidatePick::effectiveScore));

                if (best.isEmpty()) {
                    break;
                }

                SelectionCandidatePick bestPick = best.get();

                // 3.b. Persistimos el avance del puntero de rotación dentro de la offer.
                Optional<CreativeSnapshot> creative = bestPick.offer().advanceToCreative(bestPick.creative().creativeId());
                if (creative.isEmpty()) {
                    continue;
                }

                CreativeSnapshot selectedCreative = creative.get();

                // 3.c. Consumimos budget caliente antes de devolver la selección.
                OptionalLong newBalanceCents = selectionReservationService.consumeBudgetForSelection(bestPick.offer(), request, selectedCreative);
                if (newBalanceCents.isEmpty()) {
                    // Si no pudo financiarse, no repetimos este creative en esta misma iteración.
                    excludedCreatives.add(selectedCreative.creativeId());
                    continue;
                }

                // 3.d. Bloqueamos el creative localmente para el device.
                selectionReservationService.reserveCreativeForDevice(bestPick.offer(), request, selectedCreative);
                touchedOffers.add(bestPick.offer());
                excludedCreatives.add(selectedCreative.creativeId());
                selected.add(toSelectedCandidate(bestPick.offer(), selectedCreative, request));
            }

            // 4. Si hubo cambios reales en punteros, los persistimos.
            if (!touchedOffers.isEmpty()) {
                sessionOfferRepository.saveAll(touchedOffers);
            }

            return selected;
        });
    }

    private SelectedCandidate toSelectedCandidate(SessionOffer offer, CreativeSnapshot creative, CandidateSelectionRequest request) {
        // El receipt firmado permite validar después el PoP sin guardar estado extra adicional.
        String receiptId = receiptTokenService.generateReceiptId(
                offer.getSessionId(),
                offer.getId(),
                offer.getAdvertiserId(),
                creative.creativeId(),
                creative.slotCount(),
                offer.getPricePerSlot()
        );

        return new SelectedCandidate(
                offer.getId(),
                offer.getSessionId(),
                request.deviceId(),
                offer.getAdvertiserId(),
                offer.getCampaignId(),
                offer.getPricePerSlot(),
                creative.creativeId(),
                creative.mediaUrl(),
                creative.slotCount(),
                offer.getDeviceCooldownSeconds(),
                receiptId
        );
    }

}
