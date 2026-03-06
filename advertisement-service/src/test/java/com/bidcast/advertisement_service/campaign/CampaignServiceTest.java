package com.bidcast.advertisement_service.campaign;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bidcast.advertisement_service.campaign.dto.CampaignRequest;

@ExtendWith(MockitoExtension.class)
public class CampaignServiceTest {

    @Mock
    private CampaignRepository campaignRepository;

    @InjectMocks
    private CampaignService campaignService;

    @Test
    void should_returnCreatedCampaignWithDraftStatus_when_requestIsValid(){

        // Arrange
        UUID advertiserId = UUID.randomUUID();

        CampaignRequest request = new CampaignRequest(
                "Campaña de Verano",
                new BigDecimal("5000.00"),
                new BigDecimal("2.50")
        );

        Campaign savedCampaign = Campaign.builder()
                .id(UUID.randomUUID())
                .name(request.name())
                .advertiserId(advertiserId)
                .budget(request.budget())
                .bidCpm(request.bidCpm())
                .status(CampaignStatusType.DRAFT)
                .build();

        when(campaignRepository.save(any(Campaign.class)))
                .thenReturn(savedCampaign);

        // Act
        Campaign result = campaignService.createCampaign(advertiserId, request);
        assertEquals(savedCampaign.getId(), result.getId());
        
        // Assert (lo que se guardó)
        ArgumentCaptor<Campaign> campaignCaptor = ArgumentCaptor.forClass(Campaign.class);

        verify(campaignRepository).save(campaignCaptor.capture());

        Campaign capturedCampaign = campaignCaptor.getValue();

        assertEquals(request.name(), capturedCampaign.getName());
        assertEquals(advertiserId, capturedCampaign.getAdvertiserId());
        assertEquals(request.budget(), capturedCampaign.getBudget());
        assertEquals(request.bidCpm(), capturedCampaign.getBidCpm());
        assertEquals(CampaignStatusType.DRAFT, capturedCampaign.getStatus());
    }
}