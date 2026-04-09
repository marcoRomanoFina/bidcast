package com.bidcast.selection_service.selection;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bidcast.selection_service.offer.OfferStatus;
import com.bidcast.selection_service.offer.CreativeSnapshot;
import com.bidcast.selection_service.offer.SessionOffer;
import com.bidcast.selection_service.offer.SessionOfferRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
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
            List<SessionOffer> activeOffers = sessionOfferRepository.findBySessionIdAndStatus(request.sessionId(), OfferStatus.ACTIVE);
            if (activeOffers.isEmpty()) {
                return List.of();
            }

            Set<String> excludedCreatives = new HashSet<>(
                    request.excludedCreativeIds() == null ? List.of() : request.excludedCreativeIds()
            );
            List<SelectedCandidate> selected = new ArrayList<>();

            for (int i = 0; i < request.count(); i++) {
                SelectionCandidatePick best = activeOffers.stream()
                        .map(offer -> selectionScoringService.candidateForOffer(offer, request, excludedCreatives))
                        .filter(pick -> pick.creative() != null)
                        .max(Comparator.comparing(SelectionCandidatePick::effectiveScore))
                        .orElse(null);

                if (best == null) {
                    break;
                }

                CreativeSnapshot creative = best.offer().advanceToCreative(best.creative().creativeId()).orElse(null);
                if (creative == null) {
                    continue;
                }

                long newBalanceCents = selectionReservationService.reserveBudgetForSelection(best.offer(), request, creative);
                if (newBalanceCents < 0) {
                    excludedCreatives.add(creative.creativeId());
                    continue;
                }

                selectionReservationService.reserveCreativeForDevice(best.offer(), request, creative);
                excludedCreatives.add(creative.creativeId());
                selected.add(toSelectedCandidate(best.offer(), creative, request));
            }

            if (!selected.isEmpty()) {
                sessionOfferRepository.saveAll(activeOffers);
            }

            return selected;
        });
    }

    private SelectedCandidate toSelectedCandidate(SessionOffer offer, CreativeSnapshot creative, CandidateSelectionRequest request) {
        // El receipt firmado evita depender de estado extra para validar el PoP.
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
