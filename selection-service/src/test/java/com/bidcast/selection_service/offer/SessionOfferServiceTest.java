package com.bidcast.selection_service.offer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bidcast.selection_service.client.AdvertisementCampaignResponse;
import com.bidcast.selection_service.client.AdvertisementClient;
import com.bidcast.selection_service.client.AdvertisementCreativeResponse;

@ExtendWith(MockitoExtension.class)
class SessionOfferServiceTest {

    @Mock private SessionOfferRepository sessionOfferRepository;
    @Mock private AdvertisementClient advertisementClient;

    @InjectMocks
    private SessionOfferService sessionBidService;

    private CreateSessionOfferRequest request;
    private UUID campaignId;

    @BeforeEach
    void setUp() {
        campaignId = UUID.randomUUID();
        request = new CreateSessionOfferRequest(
                "session-1",
                "adv-1",
                campaignId,
                new BigDecimal("50.00"),
                new BigDecimal("2.50"),
                300
        );
    }

    @Test
    void create_persistsSessionOfferWithCreativeSnapshots() {
        AdvertisementCampaignResponse campaign = new AdvertisementCampaignResponse(
                campaignId,
                "Campaign 1",
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                new BigDecimal("2.50"),
                "ACTIVE",
                List.of(
                        new AdvertisementCreativeResponse(UUID.randomUUID(), "Creative A", "https://cdn/a.mp4", "https://a"),
                        new AdvertisementCreativeResponse(UUID.randomUUID(), "Creative B", "https://cdn/b.mp4", "https://b")
                )
        );

        CreateSessionOfferRequest request = new CreateSessionOfferRequest(
                "session-1",
                "00000000-0000-0000-0000-000000000001",
                campaignId,
                new BigDecimal("50.00"),
                new BigDecimal("2.50"),
                300
        );

        when(advertisementClient.getCampaign(campaignId)).thenReturn(campaign);
        when(sessionOfferRepository.save(org.mockito.ArgumentMatchers.any(SessionOffer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SessionOffer saved = sessionBidService.create(request);

        assertEquals(OfferStatus.ACTIVE, saved.getStatus());
        assertEquals(2, saved.getCreatives().size());
        assertEquals("https://cdn/a.mp4", saved.getCreatives().getFirst().mediaUrl());

        ArgumentCaptor<SessionOffer> captor = ArgumentCaptor.forClass(SessionOffer.class);
        verify(sessionOfferRepository).save(captor.capture());
        assertEquals(2, captor.getValue().getCreatives().size());
    }

    @Test
    void create_rejectsCampaignWithoutCreatives() {
        AdvertisementCampaignResponse campaign = new AdvertisementCampaignResponse(
                campaignId,
                "Campaign 1",
                UUID.fromString("00000000-0000-0000-0000-000000000001"),
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                new BigDecimal("2.50"),
                "ACTIVE",
                List.of()
        );

        CreateSessionOfferRequest request = new CreateSessionOfferRequest(
                "session-1",
                "00000000-0000-0000-0000-000000000001",
                campaignId,
                new BigDecimal("50.00"),
                new BigDecimal("2.50"),
                300
        );

        when(advertisementClient.getCampaign(campaignId)).thenReturn(campaign);

        assertThrows(IllegalArgumentException.class, () -> sessionBidService.create(request));
    }
}
