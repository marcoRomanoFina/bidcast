package com.bidcast.selection_service.selection;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
// Servicio de scoring del hot path.
// Decide qué creative de qué offer conviene devolver en este instante para este device.
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

    /**
     * Resuelve el mejor candidato posible dentro de una offer específica.
     *
     * No persiste nada:
     * - solo observa budget
     * - aplica exclusiones locales
     * - aplica penalty global por campaign
     * - devuelve un SelectionCandidatePick intermedio
     */
    public SelectionCandidatePick candidateForOffer(
            SessionOffer offer,
            CandidateSelectionRequest request,
            Set<String> excludedCreatives,
            Map<String, Set<String>> blockedCreativesCache,
            Map<String, BigDecimal> campaignPenaltyCache
    ) {
        // Un budget no positivo ya deja a la offer fuera de juego.
        long availableBudgetCents = availableBudgetCents(request.sessionId(), offer);
        if (availableBudgetCents <= 0) {
            return SelectionCandidatePick.empty(offer);
        }

        // Se combinan exclusiones del caller con bloqueos locales ya materializados en Redis.
        Set<String> effectiveExcluded = new HashSet<>(excludedCreatives);
        effectiveExcluded.addAll(blockedCreativeIdsForOffer(offer, request, blockedCreativesCache));
        CreativeSnapshot creative = nextAffordableCreative(offer, effectiveExcluded, availableBudgetCents).orElse(null);

        if (creative == null) {
            return SelectionCandidatePick.empty(offer);
        }

        // El score final compara valor económico ajustado por recencia global.
        BigDecimal adjustedPricePerSlot = applyCampaignPenalty(request.sessionId(), offer, campaignPenaltyCache);
        return new SelectionCandidatePick(offer, creative, scoreFor(creative, adjustedPricePerSlot));
    }

    /**
     * Responde si una offer quedó estructuralmente incapaz de financiar cualquiera de sus creatives.
     */
    public boolean isOfferPermanentlyUnaffordable(SessionOffer offer, long availableBudgetCents) {
        return offer.minimumSlotCount()
                .stream()
                .map(minSlotCount -> offer.getPricePerSlot()
                        .multiply(BigDecimal.valueOf(minSlotCount))
                        .movePointRight(2)
                        .longValueExact())
                .allMatch(minCost -> minCost > availableBudgetCents);
    }

    /**
     * Lee budget caliente y, si falta, rehidrata desde DB.
     */
    public long availableBudgetCents(String sessionId, SessionOffer offer) {
        return infrastructureService.getBudgetCents(sessionId, offer.getId().toString())
                .orElseGet(() -> rehydrationService.rehydrateOffer(offer.getId()).balanceCents());
    }

    // Obtiene los creatives de esta offer que hoy están bloqueados para ese device.
    private Set<String> blockedCreativeIdsForOffer(
            SessionOffer offer,
            CandidateSelectionRequest request,
            Map<String, Set<String>> blockedCreativesCache
    ) {
        return blockedCreativesCache.computeIfAbsent(offer.getId().toString(), ignored -> {
            List<String> creativeIds = offer.getCreatives().stream()
                    .map(CreativeSnapshot::creativeId)
                    .collect(Collectors.toList());
            return infrastructureService.filterBlockedCreativeIds(request.sessionId(), request.deviceId(), creativeIds);
        });
    }

    // Aplica una penalización suave si la misma campaign fue reproducida recientemente.
    private BigDecimal applyCampaignPenalty(String sessionId, SessionOffer offer, Map<String, BigDecimal> campaignPenaltyCache) {
        return campaignPenaltyCache.computeIfAbsent(offer.getCampaignId(), ignored -> loadCampaignAdjustedPrice(sessionId, offer));
    }

    private BigDecimal loadCampaignAdjustedPrice(String sessionId, SessionOffer offer) {
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

    // El valor económico real de un creative es pricePerSlot * slotCount.
    private BigDecimal scoreFor(CreativeSnapshot creative, BigDecimal pricePerSlot) {
        return pricePerSlot.multiply(BigDecimal.valueOf(creative.slotCount()));
    }

    /**
     * Busca el siguiente creative servible:
     * - respetando el orden circular de rotación
     * - ignorando excluded creatives
     * - chequeando affordability real contra el budget disponible
     */
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
