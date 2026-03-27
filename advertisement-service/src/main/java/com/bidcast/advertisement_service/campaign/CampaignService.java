package com.bidcast.advertisement_service.campaign;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bidcast.advertisement_service.campaign.dto.CampaignRequest;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository campaignRepository;

    @Transactional
    public Campaign createCampaign(UUID advertiserId, CampaignRequest data) {

        String normalizedName = data.name().trim();

        log.info("Creating campaign in DRAFT status: {}", data.name());

        Campaign campaign = Campaign.builder()
                .name(normalizedName)
                .advertiserId(advertiserId)
                .budget(data.budget())
                .bidCpm(data.bidCpm())
                .status(CampaignStatusType.DRAFT) 
                .build();

        Campaign saved = campaignRepository.save(campaign);
        log.info("Campaign draft stored successfully with id={}", saved.getId());

        return saved;
    }
}
