package com.bidcast.venue_service.device;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.bidcast.venue_service.device.dto.CreateDeviceRequest;
import com.bidcast.venue_service.device.dto.DeviceResponse;
import com.bidcast.venue_service.exception.GlobalExceptionHandler;
import com.bidcast.venue_service.exception.ResourceNotFoundException;

@WebMvcTest(DeviceController.class)
@Import(GlobalExceptionHandler.class)
class DeviceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeviceService deviceService;

    @Test
    void createDevice_returnsCreated() throws Exception {
        UUID id = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();

        DeviceResponse saved = DeviceResponse.builder()
            .id(id)
            .venueId(venueId)
            .deviceName("Radar")
            .createdAt(LocalDateTime.now().minusMinutes(1))
            .updatedAt(LocalDateTime.now())
            .build();

        when(deviceService.createDevice(any(CreateDeviceRequest.class))).thenReturn(saved);

        String body = """
            {
              "venueId": "%s",
              "deviceName": "Radar"
            }
            """.formatted(venueId);

        mockMvc.perform(post("/devices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.venueId").value(venueId.toString()))
            .andExpect(jsonPath("$.deviceName").value("Radar"));
    }

    @Test
    void createDevice_invalidBody_returnsBadRequest() throws Exception {
        String body = """
            {
              "venueId": "%s",
              "deviceName": "aa"
            }
            """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/devices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.deviceName").value("Device name must be between 3 and 100 characters"));
    }

    @Test
    void getDeviceById_returnsOk() throws Exception {
        UUID id = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();

        DeviceResponse device = DeviceResponse.builder()
            .id(id)
            .venueId(venueId)
            .deviceName("Sensor X")
            .createdAt(LocalDateTime.now().minusHours(2))
            .updatedAt(LocalDateTime.now())
            .build();

        when(deviceService.getDeviceById(id)).thenReturn(device);

        mockMvc.perform(get("/devices/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.venueId").value(venueId.toString()))
            .andExpect(jsonPath("$.deviceName").value("Sensor X"));
    }

    @Test
    void getDeviceById_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(deviceService.getDeviceById(id))
            .thenThrow(new ResourceNotFoundException("Device not found: " + id));

        mockMvc.perform(get("/devices/{id}", id))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Device not found: " + id));
    }

    @Test
    void deleteDevice_returnsNoContent() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/devices/{id}", id))
            .andExpect(status().isNoContent());

        verify(deviceService).deleteDevice(id);
    }

    @Test
    void deleteDevice_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("Device not found: " + id))
            .when(deviceService).deleteDevice(id);

        mockMvc.perform(delete("/devices/{id}", id))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Device not found: " + id));
    }

    @Test
    void getDevicesByVenue_returnsList() throws Exception {
        UUID venueId = UUID.randomUUID();
        DeviceResponse first = DeviceResponse.builder()
            .id(UUID.randomUUID())
            .venueId(venueId)
            .deviceName("Display North")
            .build();

        DeviceResponse second = DeviceResponse.builder()
            .id(UUID.randomUUID())
            .venueId(venueId)
            .deviceName("Display South")
            .build();

        when(deviceService.getDevicesByVenue(venueId)).thenReturn(List.of(first, second));

        mockMvc.perform(get("/devices/venue/{venueId}", venueId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].deviceName").value("Display North"))
            .andExpect(jsonPath("$[1].deviceName").value("Display South"));
    }
}
