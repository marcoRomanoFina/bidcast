package com.bidcast.advertisement_service.campaign;

import com.bidcast.advertisement_service.campaign.dto.CampaignRequest;

import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CampaignController.class)
class CampaignControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CampaignService campaignService;

    @Test
    void should_return201Created_when_requestIsValid() throws Exception {
        UUID advertiserId = UUID.randomUUID();
        CampaignRequest request = new CampaignRequest(
                "Campaña Verano",
                new BigDecimal("1500.00"),
                new BigDecimal("3.50")
        );

        Campaign savedCampaign = Campaign.builder()
                .id(UUID.randomUUID())
                .name(request.name())
                .advertiserId(advertiserId)
                .budget(request.budget())
                .spent(BigDecimal.ZERO)
                .bidCpm(request.bidCpm())
                .status(CampaignStatusType.DRAFT)
                .build();

        when(campaignService.createCampaign(eq(advertiserId), eq(request))).thenReturn(savedCampaign);

        mockMvc.perform(post("/api/campaigns")
                        .header("X-User-Id", advertiserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(savedCampaign.getId().toString()))
                .andExpect(jsonPath("$.advertiserId").value(advertiserId.toString()))
                .andExpect(jsonPath("$.name").value("Campaña Verano"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void should_return400BadRequest_when_requestHasInvalidData() throws Exception {
        // Arrange
        UUID advertiserId = UUID.randomUUID();
        CampaignRequest invalidRequest = new CampaignRequest(
                "", 
                new BigDecimal("-100.00"), 
                new BigDecimal("0.00") 
        );

        // Act & Assert
        mockMvc.perform(post("/api/campaigns")
                        .header("X-User-Id", advertiserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                
                // Aseguramos que devuelva HTTP 400
                .andExpect(status().isBadRequest())
                
                // Verificamos que Map<String, String> traiga las claves correctas que fallaron
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.budget").exists())
                .andExpect(jsonPath("$.bidCpm").exists())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void should_return400BadRequest_when_userHeaderIsMissing() throws Exception {
        CampaignRequest request = new CampaignRequest(
                "Campaña Header",
                new BigDecimal("1000.00"),
                new BigDecimal("2.00")
        );

        mockMvc.perform(post("/api/campaigns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.['X-User-Id']").value("Header is required"));
    }

    @Test
    void should_return400BadRequest_when_userHeaderHasInvalidUuid() throws Exception {
        CampaignRequest request = new CampaignRequest(
                "Campaña Header",
                new BigDecimal("1000.00"),
                new BigDecimal("2.00")
        );

        mockMvc.perform(post("/api/campaigns")
                        .header("X-User-Id", "not-a-uuid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.['X-User-Id']").value("Invalid format"));
    }

    @Test
    void should_returnCampaignDetails_when_campaignExists() throws Exception {
        UUID campaignId = UUID.randomUUID();
        UUID advertiserId = UUID.randomUUID();

        Campaign campaign = Campaign.builder()
                .id(campaignId)
                .name("Campaña Snapshot")
                .advertiserId(advertiserId)
                .budget(new BigDecimal("2500.00"))
                .spent(new BigDecimal("250.00"))
                .bidCpm(new BigDecimal("4.50"))
                .status(CampaignStatusType.ACTIVE)
                .creatives(java.util.List.of(
                        Creative.builder()
                                .id(UUID.randomUUID())
                                .name("Creative A")
                                .mediaUrl("https://cdn/a.mp4")
                                .clickUrl("https://click/a")
                                .build(),
                        Creative.builder()
                                .id(UUID.randomUUID())
                                .name("Creative B")
                                .mediaUrl("https://cdn/b.mp4")
                                .clickUrl("https://click/b")
                                .build()
                ))
                .build();

        when(campaignService.getCampaign(eq(campaignId))).thenReturn(campaign);

        mockMvc.perform(get("/api/campaigns/{campaignId}", campaignId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(campaignId.toString()))
                .andExpect(jsonPath("$.advertiserId").value(advertiserId.toString()))
                .andExpect(jsonPath("$.name").value("Campaña Snapshot"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.creatives.length()").value(2))
                .andExpect(jsonPath("$.creatives[0].mediaUrl").value("https://cdn/a.mp4"));
    }
}
