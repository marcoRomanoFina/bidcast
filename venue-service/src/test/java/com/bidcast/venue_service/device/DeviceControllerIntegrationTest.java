package com.bidcast.venue_service.device;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import com.bidcast.venue_service.venue.Venue;
import com.bidcast.venue_service.venue.VenueCategory;
import com.bidcast.venue_service.venue.VenueRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeviceControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private VenueRepository venueRepository;

    private Venue testVenue;

    @BeforeEach
    void cleanDatabase() {
        deviceRepository.deleteAll();
        venueRepository.deleteAll();
        
        testVenue = venueRepository.save(Venue.builder()
                .name("Test Venue")
                .ownerId(UUID.randomUUID())
                .category(VenueCategory.OTHER)
                .build());
    }

    @Test
    void createDevice_persistsAndReturnsCreated() throws Exception {
        String body = """
            {
              "venueId": "%s",
              "deviceName": "Screen Prime"
            }
            """.formatted(testVenue.getId());

        mockMvc.perform(post("/devices")
                .contentType("application/json")
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.venueId").value(testVenue.getId().toString()))
            .andExpect(jsonPath("$.deviceName").value("Screen Prime"));

        org.junit.jupiter.api.Assertions.assertEquals(1, deviceRepository.count());
        Device saved = deviceRepository.findAll().getFirst();
        org.junit.jupiter.api.Assertions.assertEquals(testVenue.getId(), saved.getVenue().getId());
        org.junit.jupiter.api.Assertions.assertEquals("Screen Prime", saved.getDeviceName());
    }

    @Test
    void createDevice_invalidPayload_returnsBadRequest() throws Exception {
        String body = """
            {
              "venueId": "%s",
              "deviceName": "aa"
            }
            """.formatted(testVenue.getId());

        mockMvc.perform(post("/devices")
                .contentType("application/json")
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.deviceName").value("Device name must be between 3 and 100 characters"));
    }

    @Test
    void getDeviceById_returnsPersistedDevice() throws Exception {
        Device device = deviceRepository.save(Device.builder()
                .venue(testVenue)
                .deviceName("Outdoor Panel")
                .build());

        mockMvc.perform(get("/devices/{id}", device.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(device.getId().toString()))
            .andExpect(jsonPath("$.venueId").value(testVenue.getId().toString()))
            .andExpect(jsonPath("$.deviceName").value("Outdoor Panel"));
    }

    @Test
    void getDeviceById_whenMissing_returns404() throws Exception {
        UUID missingId = UUID.randomUUID();

        mockMvc.perform(get("/devices/{id}", missingId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Device not found: " + missingId));
    }

    @Test
    void deleteDevice_removesPersistedDevice() throws Exception {
        Device device = deviceRepository.save(Device.builder()
                .venue(testVenue)
                .deviceName("Legacy Sign")
                .build());

        mockMvc.perform(delete("/devices/{id}", device.getId()))
            .andExpect(status().isNoContent());

        org.junit.jupiter.api.Assertions.assertFalse(deviceRepository.existsById(device.getId()));
    }

    @Test
    void getDevicesByVenue_returnsOnlyMatchingVenueDevices() throws Exception {
        Venue otherVenue = venueRepository.save(Venue.builder()
                .name("Other Venue")
                .ownerId(UUID.randomUUID())
                .category(VenueCategory.OTHER)
                .build());

        deviceRepository.save(Device.builder()
                .venue(testVenue)
                .deviceName("North Screen")
                .build());

        deviceRepository.save(Device.builder()
                .venue(testVenue)
                .deviceName("South Screen")
                .build());

        deviceRepository.save(Device.builder()
                .venue(otherVenue)
                .deviceName("Foreign Screen")
                .build());

        mockMvc.perform(get("/devices/venue/{venueId}", testVenue.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].venueId").value(testVenue.getId().toString()))
            .andExpect(jsonPath("$[1].venueId").value(testVenue.getId().toString()));
    }
}
