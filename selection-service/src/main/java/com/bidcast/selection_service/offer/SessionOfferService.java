package com.bidcast.selection_service.offer;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bidcast.selection_service.client.AdvertisementCampaignResponse;
import com.bidcast.selection_service.client.AdvertisementClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SessionOfferService {

    private final SessionOfferRepository sessionOfferRepository;
    private final AdvertisementClient advertisementClient;

    @Transactional
    public SessionOffer create(CreateSessionOfferRequest request) {
        AdvertisementCampaignResponse campaign = advertisementClient.getCampaign(request.campaignId());

        if (campaign.creatives() == null || campaign.creatives().isEmpty()) {
            throw new IllegalArgumentException("Campaign has no creatives: " + request.campaignId());
        }

        if (!request.advertiserId().equals(campaign.advertiserId().toString())) {
            throw new IllegalArgumentException("Campaign does not belong to advertiser: " + request.advertiserId());
        }

        List<CreativeSnapshot> snapshots = campaign.creatives().stream()
                .map(creative -> new CreativeSnapshot(creative.creativeId().toString(), creative.mediaUrl(), 1))
                .toList();

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

        SessionOffer saved = sessionOfferRepository.save(offer);
        log.info("Session offer {} created for session {} with {} creatives", saved.getId(), saved.getSessionId(), saved.getCreatives().size());
        return saved;
    }
}
