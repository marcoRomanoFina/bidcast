package com.bidcast.device_service.device;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(properties = {
        "spring.profiles.active=test",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class DeviceControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DeviceRepository deviceRepository;

    @BeforeEach
    void cleanDatabase() {
        deviceRepository.deleteAll();
    }

    @Test
    void createDevice_persistsAndReturnsCreated() throws Exception {
        UUID ownerId = UUID.randomUUID();
        String body = """
            {
              "ownerId": "%s",
              "deviceName": "Screen Prime"
            }
            """.formatted(ownerId);

        mockMvc.perform(post("/devices")
                .contentType("application/json")
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.ownerId").value(ownerId.toString()))
            .andExpect(jsonPath("$.deviceName").value("Screen Prime"));

        org.junit.jupiter.api.Assertions.assertEquals(1, deviceRepository.count());
        Device saved = deviceRepository.findAll().getFirst();
        org.junit.jupiter.api.Assertions.assertEquals(ownerId, saved.getOwnerId());
        org.junit.jupiter.api.Assertions.assertEquals("Screen Prime", saved.getDeviceName());
    }

    @Test
    void createDevice_invalidPayload_returnsBadRequest() throws Exception {
        UUID ownerId = UUID.randomUUID();
        String body = """
            {
              "ownerId": "%s",
              "deviceName": "aa"
            }
            """.formatted(ownerId);

        mockMvc.perform(post("/devices")
                .contentType("application/json")
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Device name must be between 3 and 100 characters"));
    }

    @Test
    void getDeviceById_returnsPersistedDevice() throws Exception {
        Device device = deviceRepository.save(Device.builder()
                .ownerId(UUID.randomUUID())
                .deviceName("Outdoor Panel")
                .build());

        mockMvc.perform(get("/devices/{id}", device.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(device.getId().toString()))
            .andExpect(jsonPath("$.ownerId").value(device.getOwnerId().toString()))
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
    void getDeviceById_invalidUuid_returns400() throws Exception {
        mockMvc.perform(get("/devices/{id}", "uuid-invalido"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid format for id"));
    }

    @Test
    void deleteDevice_removesPersistedDevice() throws Exception {
        Device device = deviceRepository.save(Device.builder()
                .ownerId(UUID.randomUUID())
                .deviceName("Legacy Sign")
                .build());

        mockMvc.perform(delete("/devices/{id}", device.getId()))
            .andExpect(status().isNoContent());

        org.junit.jupiter.api.Assertions.assertFalse(deviceRepository.existsById(device.getId()));
    }

    @Test
    void deleteDevice_whenMissing_returns404() throws Exception {
        UUID missingId = UUID.randomUUID();

        mockMvc.perform(delete("/devices/{id}", missingId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Device not found: " + missingId));
    }

    @Test
    void getDevicesByOwner_returnsOnlyMatchingOwnerDevices() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID otherOwnerId = UUID.randomUUID();

        deviceRepository.save(Device.builder()
                .ownerId(ownerId)
                .deviceName("North Screen")
                .createdAt(LocalDateTime.now().minusDays(1))
                .build());

        deviceRepository.save(Device.builder()
                .ownerId(ownerId)
                .deviceName("South Screen")
                .createdAt(LocalDateTime.now().minusHours(6))
                .build());

        deviceRepository.save(Device.builder()
                .ownerId(otherOwnerId)
                .deviceName("Foreign Screen")
                .build());

        mockMvc.perform(get("/devices/owner/{ownerId}", ownerId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].ownerId").value(ownerId.toString()))
            .andExpect(jsonPath("$[1].ownerId").value(ownerId.toString()));
    }
}
