package com.bidcast.venue_service.venue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.bidcast.venue_service.exception.GlobalExceptionHandler;
import com.bidcast.venue_service.venue.dto.CreateVenueRequest;
import com.bidcast.venue_service.venue.dto.VenueResponse;

@WebMvcTest(VenueController.class)
@Import(GlobalExceptionHandler.class)
class VenueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VenueService venueService;

    @Test
    void createVenue_returnsCreated() throws Exception {
        UUID id = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        VenueCategory category = VenueCategory.GYM;

        VenueResponse saved = VenueResponse.builder()
                .id(id)
                .name("Smart Fit")
                .ownerId(ownerId)
                .category(category)
                .build();

        when(venueService.createVenue(any(CreateVenueRequest.class))).thenReturn(saved);

        String body = """
                {
                  "name": "Smart Fit",
                  "ownerId": "%s",
                  "category": "GYM"
                }
                """.formatted(ownerId);

        mockMvc.perform(post("/venues")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Smart Fit"))
                .andExpect(jsonPath("$.category").value("GYM"));
    }

    @Test
    void getVenueById_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        VenueResponse response = VenueResponse.builder()
                .id(id)
                .name("Gym X")
                .category(VenueCategory.GYM)
                .build();

        when(venueService.getVenueById(id)).thenReturn(response);

        mockMvc.perform(get("/venues/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("Gym X"));
    }

    @Test
    void getVenuesByOwner_returnsList() throws Exception {
        UUID ownerId = UUID.randomUUID();
        VenueResponse v1 = VenueResponse.builder().id(UUID.randomUUID()).name("V1").build();
        VenueResponse v2 = VenueResponse.builder().id(UUID.randomUUID()).name("V2").build();

        when(venueService.getVenuesByOwner(ownerId)).thenReturn(List.of(v1, v2));

        mockMvc.perform(get("/venues/owner/{ownerId}", ownerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("V1"))
                .andExpect(jsonPath("$[1].name").value("V2"));
    }
}
