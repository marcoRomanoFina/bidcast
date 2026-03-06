package com.bidcast.advertisement_service.campaign;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bidcast.advertisement_service.campaign.dto.CampaignRequest;


@Service
@RequiredArgsConstructor
public class CampaignService {

    private final CampaignRepository campaignRepository;

    @Transactional
    public Campaign createCampaign(CampaignRequest data) {
        
        Campaign campaign = Campaign.builder()
                .name(data.name())
                .budget(data.budget())
                .status(CampaignStatusType.DRAFT) 
                .build();

        return campaignRepository.save(campaign);
    }
}