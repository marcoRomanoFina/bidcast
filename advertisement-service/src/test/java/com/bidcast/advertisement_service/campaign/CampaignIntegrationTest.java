package com.bidcast.advertisement_service.campaign;

import com.bidcast.advertisement_service.campaign.dto.CampaignRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.client.RestTestClient; 
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Testcontainers
class CampaignIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private RestTestClient restClient; 

    @Autowired
    private CampaignRepository campaignRepository;

    @BeforeEach
    void setUp() {
        campaignRepository.deleteAll();
    }

    @Test
    void should_saveCampaignInRealDatabase_when_requestIsValid() {
        // Arrange
        UUID advertiserId = UUID.randomUUID();
        CampaignRequest request = new CampaignRequest(
                "Campaña Black Friday",
                new BigDecimal("10000.00"),
                new BigDecimal("5.00")
        );

        // Act & Assert 
        restClient.post()
                .uri("/api/campaigns")
                .header("X-Advertiser-Id", advertiserId.toString())
                .body(request) 
                .exchange()
                .expectStatus().isCreated(); 

        // Verificación en BD
        var savedCampaigns = campaignRepository.findAll();
        assertEquals(1, savedCampaigns.size());
        assertEquals("Campaña Black Friday", savedCampaigns.get(0).getName());
    }
}