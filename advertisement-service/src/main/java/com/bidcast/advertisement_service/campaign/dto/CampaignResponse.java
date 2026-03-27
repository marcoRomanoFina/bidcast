package com.bidcast.advertisement_service.campaign.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.bidcast.advertisement_service.campaign.Campaign;
import com.bidcast.advertisement_service.campaign.CampaignStatusType;

public record CampaignResponse(
        UUID id,
        String name,
        UUID advertiserId,
        BigDecimal budget,
        BigDecimal spent,
        BigDecimal bidCpm,
        CampaignStatusType status,
        Instant createdAt,
        Instant updatedAt
) {
    
    public static CampaignResponse from(Campaign campaign) {
        return new CampaignResponse(
                campaign.getId(),
                campaign.getName(),
                campaign.getAdvertiserId(),
                campaign.getBudget(),
                campaign.getSpent(),
                campaign.getBidCpm(),
                campaign.getStatus(),
                campaign.getCreatedAt(),
                campaign.getUpdatedAt()
        );
    }
}
