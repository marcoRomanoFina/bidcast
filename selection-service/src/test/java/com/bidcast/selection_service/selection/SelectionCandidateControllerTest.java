package com.bidcast.selection_service.selection;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.bidcast.selection_service.core.exception.SelectionInfrastructureUnavailableException;
import com.bidcast.selection_service.core.exception.SessionSelectionBusyException;
import com.bidcast.selection_service.core.exception.GlobalExceptionHandler;

@ExtendWith(MockitoExtension.class)
class SelectionCandidateControllerTest {

    @Mock
    private SelectionCandidateService selectionCandidateService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new SelectionCandidateController(selectionCandidateService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void selectCandidates_returnsResolvedCandidates() throws Exception {
        when(selectionCandidateService.selectCandidates(any())).thenReturn(List.of(
                new SelectedCandidate(
                        UUID.randomUUID(),
                        "session-1",
                        "device-1",
                        "advertiser-1",
                        "campaign-1",
                        new BigDecimal("10.00"),
                        "creative-1",
                        "https://cdn/creative-1.mp4",
                        1,
                        300,
                        "receipt-1"
                )
        ));

        mockMvc.perform(post("/api/v1/selection/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sessionId": "session-1",
                                  "deviceId": "device-1",
                                  "count": 1,
                                  "excludedCreativeIds": []
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].campaignId").value("campaign-1"))
                .andExpect(jsonPath("$[0].creativeId").value("creative-1"))
                .andExpect(jsonPath("$[0].deviceCooldownSeconds").value(300));
    }

    @Test
    void selectCandidates_returnsConflictWhenSessionIsBusy() throws Exception {
        when(selectionCandidateService.selectCandidates(any()))
                .thenThrow(new SessionSelectionBusyException("session-1"));

        mockMvc.perform(post("/api/v1/selection/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sessionId": "session-1",
                                  "deviceId": "device-1",
                                  "count": 1,
                                  "excludedCreativeIds": []
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ERR_DOMAIN_POLICY"));
    }

    @Test
    void selectCandidates_returnsServiceUnavailableWhenRedisIsDown() throws Exception {
        when(selectionCandidateService.selectCandidates(any()))
                .thenThrow(new SelectionInfrastructureUnavailableException("Redis"));

        mockMvc.perform(post("/api/v1/selection/candidates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sessionId": "session-1",
                                  "deviceId": "device-1",
                                  "count": 1,
                                  "excludedCreativeIds": []
                                }
                                """))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.errorCode").value("ERR_INFRA_UNAVAILABLE"));
    }
}
