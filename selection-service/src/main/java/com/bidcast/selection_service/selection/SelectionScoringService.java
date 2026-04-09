package com.bidcast.selection_service.selection;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.bidcast.selection_service.offer.CreativeSnapshot;
import com.bidcast.selection_service.offer.OfferInfrastructureService;
import com.bidcast.selection_service.offer.OfferRehydrationService;
import com.bidcast.selection_service.offer.SessionOffer;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SelectionScoringService {

    /**
     * Resuelve que creative conviene devolver para una offer dada.
     * Combina budget disponible, exclusions locales del device
     * y una penalizacion global suave por campaign reciente.
     */
    private static final Duration CAMPAIGN_PENALTY_WINDOW = Duration.ofMinutes(5);
    private static final BigDecimal RECENT_CAMPAIGN_MULTIPLIER = new BigDecimal("0.75");

    private final OfferInfrastructureService infrastructureService;
    private final OfferRehydrationService rehydrationService;
    private final SelectionPricingService selectionPricingService;
    private final StringRedisTemplate redisTemplate;

    public SelectionCandidatePick candidateForOffer(
            SessionOffer offer,
            CandidateSelectionRequest request,
            Set<String> excludedCreatives
    ) {
        long availableBudgetCents = availableBudgetCents(request.sessionId(), offer);
        if (availableBudgetCents <= 0) {
            return SelectionCandidatePick.empty(offer);
        }

        Set<String> effectiveExcluded = new HashSet<>(excludedCreatives);
        effectiveExcluded.addAll(blockedCreativeIdsForOffer(offer, request));
        CreativeSnapshot creative = nextAffordableCreative(offer, effectiveExcluded, availableBudgetCents).orElse(null);

        if (creative == null) {
            return SelectionCandidatePick.empty(offer);
        }

        BigDecimal adjustedPricePerSlot = applyCampaignPenalty(request.sessionId(), offer);
        return new SelectionCandidatePick(offer, creative, scoreFor(creative, adjustedPricePerSlot));
    }

    public boolean isOfferPermanentlyUnaffordable(SessionOffer offer, long availableBudgetCents) {
        return offer.getCreatives().stream()
                .mapToLong(creative -> selectionPricingService.selectionCostCents(offer, creative))
                .min()
                .stream()
                .allMatch(minCost -> minCost > availableBudgetCents);
    }

    public long availableBudgetCents(String sessionId, SessionOffer offer) {
        return infrastructureService.getBudgetCents(sessionId, offer.getId().toString())
                .orElseGet(() -> rehydrationService.rehydrateOffer(offer.getId()).balanceCents());
    }

    private Set<String> blockedCreativeIdsForOffer(SessionOffer offer, CandidateSelectionRequest request) {
        List<String> creativeIds = offer.getCreatives().stream()
                .map(CreativeSnapshot::creativeId)
                .collect(Collectors.toList());
        return infrastructureService.filterBlockedCreativeIds(request.sessionId(), request.deviceId(), creativeIds);
    }

    private BigDecimal applyCampaignPenalty(String sessionId, SessionOffer offer) {
        // La recencia global ajusta score, pero no bloquea campaigns de forma dura.
        String key = String.format("session:%s:campaign:%s:last_played", sessionId, offer.getCampaignId());
        String rawLastPlayed = redisTemplate.opsForValue().get(key);

        if (rawLastPlayed == null) {
            return offer.getPricePerSlot();
        }

        long lastPlayedMillis = Long.parseLong(rawLastPlayed);
        Duration sinceLastPlay = Duration.between(Instant.ofEpochMilli(lastPlayedMillis), Instant.now());

        if (sinceLastPlay.compareTo(CAMPAIGN_PENALTY_WINDOW) < 0) {
            return offer.getPricePerSlot().multiply(RECENT_CAMPAIGN_MULTIPLIER);
        }

        return offer.getPricePerSlot();
    }

    private BigDecimal scoreFor(CreativeSnapshot creative, BigDecimal pricePerSlot) {
        return pricePerSlot.multiply(BigDecimal.valueOf(creative.slotCount()));
    }

    private Optional<CreativeSnapshot> nextAffordableCreative(SessionOffer offer, Set<String> excludedCreativeIds, long availableBudgetCents) {
        if (offer.getCreatives() == null || offer.getCreatives().isEmpty()) {
            return Optional.empty();
        }

        // Recorre en orden circular para mantener rotacion estable entre creatives de la misma offer.
        Set<String> excluded = excludedCreativeIds == null ? Set.of() : excludedCreativeIds;
        int startIndex = Math.floorMod(
                offer.getNextCreativeIndex() == null ? 0 : offer.getNextCreativeIndex(),
                offer.getCreatives().size()
        );

        for (int offset = 0; offset < offer.getCreatives().size(); offset++) {
            int candidateIndex = (startIndex + offset) % offer.getCreatives().size();
            CreativeSnapshot candidate = offer.getCreatives().get(candidateIndex);
            if (excluded.contains(candidate.creativeId())) {
                continue;
            }

            if (selectionPricingService.selectionCostCents(offer, candidate) <= availableBudgetCents) {
                return Optional.of(candidate);
            }
        }

        return Optional.empty();
    }
}
