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
        
        log.info("Creando nueva campaña en estado DRAFT: {}", data.name());

        Campaign campaign = Campaign.builder()
                .name(data.name())
                .advertiserId(advertiserId)
                .budget(data.budget())
                .bidCpm(data.bidCpm())
                .status(CampaignStatusType.DRAFT) 
                .build();

        Campaign saved = campaignRepository.save(campaign);
        log.info("Borrador guardado exitosamente con ID: {}", saved.getId());

        return saved;
    }
}