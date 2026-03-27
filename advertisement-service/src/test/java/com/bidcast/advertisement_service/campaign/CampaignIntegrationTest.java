package com.bidcast.advertisement_service.campaign;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.profiles.active=test",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@Testcontainers
class CampaignIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CampaignRepository campaignRepository;

    @BeforeEach
    void setUp() {
        campaignRepository.deleteAll();
    }

    @Test
    void should_saveCampaignInRealDatabase_when_requestIsValid() throws Exception {
        // Arrange
        UUID advertiserId = UUID.randomUUID();

        // Act & Assert 
        String body = """
                {
                  "name": "Campaña Black Friday",
                  "budget": 10000.00,
                  "bidCpm": 5.00
                }
                """;

        mockMvc.perform(post("/api/campaigns")
                .header("X-User-Id", advertiserId.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.advertiserId").value(advertiserId.toString()))
            .andExpect(jsonPath("$.status").value("DRAFT"));

        // Verificación en DB
        var savedCampaigns = campaignRepository.findAll();
        assertEquals(1, savedCampaigns.size());
        Campaign saved = savedCampaigns.getFirst();
        assertEquals("Campaña Black Friday", saved.getName());
        assertEquals(advertiserId, saved.getAdvertiserId());
        assertEquals(CampaignStatusType.DRAFT, saved.getStatus());
        assertEquals(0, saved.getSpent().compareTo(BigDecimal.ZERO));
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    void should_return400AndNotPersist_when_requestIsInvalid() throws Exception {
        String body = """
                {
                  "name": "",
                  "budget": -100.00,
                  "bidCpm": 0.00
                }
                """;

        mockMvc.perform(post("/api/campaigns")
                .header("X-User-Id", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.name").exists())
            .andExpect(jsonPath("$.budget").exists())
            .andExpect(jsonPath("$.bidCpm").exists());

        assertEquals(0, campaignRepository.count());
    }

    @Test
    void should_return400_when_userHeaderIsMissing() throws Exception {
        String body = """
                {
                  "name": "Campaña Header Missing",
                  "budget": 800.00,
                  "bidCpm": 1.50
                }
                """;

        mockMvc.perform(post("/api/campaigns")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.['X-User-Id']").value("Header is required"));

        assertEquals(0, campaignRepository.count());
    }

    @Test
    void should_return400_when_userHeaderIsInvalidUuid() throws Exception {
        String body = """
                {
                  "name": "Campaña Header Invalid",
                  "budget": 800.00,
                  "bidCpm": 1.50
                }
                """;

        mockMvc.perform(post("/api/campaigns")
                .header("X-User-Id", "bad-uuid")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.['X-User-Id']").value("Invalid format"));

        assertEquals(0, campaignRepository.count());
    }
}
