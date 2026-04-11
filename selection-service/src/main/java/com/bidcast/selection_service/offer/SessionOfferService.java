package com.bidcast.selection_service.offer;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bidcast.selection_service.client.AdvertisementCampaignResponse;
import com.bidcast.selection_service.client.AdvertisementClient;
import com.bidcast.selection_service.core.exception.OfferPriceBelowSessionBasePriceException;
import com.bidcast.selection_service.session.ActiveSessionSnapshot;
import com.bidcast.selection_service.session.SelectionSessionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
// Servicio de alta de offers.
// Traduce una campaign externa del advertisement-service en una SessionOffer local,
// con snapshots de creatives listos para el hot path.
public class SessionOfferService {

    private final SessionOfferRepository sessionOfferRepository;
    private final AdvertisementClient advertisementClient;
    private final SelectionSessionService selectionSessionService;

    @Transactional
    public SessionOffer create(CreateSessionOfferRequest request) {
        ActiveSessionSnapshot sessionSnapshot = selectionSessionService.getRequiredActiveSession(request.sessionId());

        if (request.pricePerSlot().compareTo(sessionSnapshot.getBasePricePerSlot()) < 0) {
            throw new OfferPriceBelowSessionBasePriceException(
                    request.sessionId(),
                    request.pricePerSlot(),
                    sessionSnapshot.getBasePricePerSlot()
            );
        }

        // 1. Pedimos el snapshot remoto de la campaign.
        AdvertisementCampaignResponse campaign = advertisementClient.getCampaign(request.campaignId());

        // 2. Validamos que realmente exista material para servir.
        if (campaign.creatives() == null || campaign.creatives().isEmpty()) {
            throw new IllegalArgumentException("Campaign has no creatives: " + request.campaignId());
        }

        // 3. Verificamos ownership para que un advertiser no registre campaigns ajenas.
        if (!request.advertiserId().equals(campaign.advertiserId().toString())) {
            throw new IllegalArgumentException("Campaign does not belong to advertiser: " + request.advertiserId());
        }

        // 4. Generamos snapshots locales.
        // Hoy cada creative entra con slotCount = 1 por simplicidad base.
        List<CreativeSnapshot> snapshots = campaign.creatives().stream()
                .map(creative -> new CreativeSnapshot(creative.creativeId().toString(), creative.mediaUrl(), 1))
                .toList();

        // 5. Construimos la offer local que va a vivir dentro de la session.
        SessionOffer offer = SessionOffer.builder()
                .sessionId(request.sessionId())
                .advertiserId(request.advertiserId())
                .campaignId(request.campaignId().toString())
                .totalBudget(request.totalBudget())
                .pricePerSlot(request.pricePerSlot())
                .deviceCooldownSeconds(request.deviceCooldownSeconds())
                .creatives(snapshots)
                .status(OfferStatus.ACTIVE)
                .build();

        // 6. Persistimos y devolvemos la versión ya identificada.
        SessionOffer saved = sessionOfferRepository.save(offer);
        log.info("Session offer {} created for session {} with {} creatives", saved.getId(), saved.getSessionId(), saved.getCreatives().size());
        return saved;
    }
}
