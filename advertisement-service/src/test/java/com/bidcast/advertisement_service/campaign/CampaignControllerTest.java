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
                        .header("X-Advertiser-Id", advertiserId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                
                // Aseguramos que devuelva HTTP 400
                .andExpect(status().isBadRequest())
                
                // Verificamos que tu Map<String, String> traiga las claves correctas que fallaron
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.budget").exists())
                .andExpect(jsonPath("$.bidCpm").exists())
                .andExpect(jsonPath("$.length()").value(3));
    }
}