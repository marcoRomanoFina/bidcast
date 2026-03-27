package com.bidcast.auction_service.core.exception;

import com.bidcast.auction_service.bid.BidController;
import com.bidcast.auction_service.bid.BidRegistrationService;
import com.bidcast.auction_service.pop.ProofOfPlayController;
import com.bidcast.auction_service.pop.ProofOfPlayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerWebMvcTest {

    @Mock
    private ProofOfPlayService proofOfPlayService;

    @Mock
    private BidRegistrationService bidRegistrationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new ProofOfPlayController(proofOfPlayService),
                        new BidController(bidRegistrationService)
                )
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("Body inválido en PoP responde 400 y no 500")
    void shouldReturn400WhenPopPayloadIsInvalid() throws Exception {
        String invalidBody = """
                {
                  "sessionId": "",
                  "bidId": "",
                  "playReceiptId": ""
                }
                """;

        mockMvc.perform(post("/api/v1/auction/pop")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ERR_VALIDATION"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("sessionId")));
    }

    @Test
    @DisplayName("JSON mal formado responde 400")
    void shouldReturn400WhenBodyIsMalformed() throws Exception {
        mockMvc.perform(post("/api/v1/auction/pop")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ERR_BAD_REQUEST_BODY"));
    }

    @Test
    @DisplayName("Body inválido en registro de bid responde 400")
    void shouldReturn400WhenBidPayloadIsInvalid() throws Exception {
        String invalidBody = """
                {
                  "sessionId": "",
                  "advertiserId": "",
                  "campaignId": "",
                  "totalBudget": 0,
                  "advertiserBidPrice": -1,
                  "mediaUrl": ""
                }
                """;

        mockMvc.perform(post("/api/v1/bids")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ERR_VALIDATION"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("totalBudget")));
    }
}
