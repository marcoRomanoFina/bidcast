package com.bidcast.device_service.device;

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

import com.bidcast.device_service.device.dto.CreateDeviceRequest;
import com.bidcast.device_service.device.dto.DeviceResponse;
import com.bidcast.device_service.exception.DeviceNotFoundException;
import com.bidcast.device_service.exception.GlobalExceptionHandler;

@WebMvcTest(DeviceController.class)
@Import(GlobalExceptionHandler.class)
class DeviceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeviceService deviceService;

    @Test
    void createDevice_returnsCreated() throws Exception {
        // Arrange: mock del service con entidad guardada.
        UUID id = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        DeviceResponse saved = DeviceResponse.builder()
            .id(id)
            .ownerId(ownerId)
            .deviceName("Radar")
            .createdAt(LocalDateTime.now().minusMinutes(1))
            .updatedAt(LocalDateTime.now())
            .build();

        when(deviceService.createDevice(any(CreateDeviceRequest.class))).thenReturn(saved);

        // Act + Assert: POST valida y devuelve 201 con body esperado.
        String body = """
            {
              "ownerId": "%s",
              "deviceName": "Radar"
            }
            """.formatted(ownerId);

        mockMvc.perform(post("/devices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.ownerId").value(ownerId.toString()))
            .andExpect(jsonPath("$.deviceName").value("Radar"));
    }

    @Test
    void createDevice_invalidBody_returnsBadRequest() throws Exception {
        // Act + Assert: body invalido dispara validaciones.
        String body = """
            {
              "ownerId": "%s",
              "deviceName": "aa"
            }
            """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/devices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Device name must be between 3 and 100 characters"));
    }

    @Test
    void getDeviceById_returnsOk() throws Exception {
        // Arrange: service devuelve entidad.
        UUID id = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        DeviceResponse device = DeviceResponse.builder()
            .id(id)
            .ownerId(ownerId)
            .deviceName("Sensor X")
            .createdAt(LocalDateTime.now().minusHours(2))
            .updatedAt(LocalDateTime.now())
            .build();

        when(deviceService.getDeviceById(id)).thenReturn(device);

        // Act + Assert
        mockMvc.perform(get("/devices/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.ownerId").value(ownerId.toString()))
            .andExpect(jsonPath("$.deviceName").value("Sensor X"));
    }

    @Test
    void getDeviceById_notFound_returns404() throws Exception {
        // Arrange: service lanza exception custom.
        UUID id = UUID.randomUUID();
        when(deviceService.getDeviceById(id))
            .thenThrow(new DeviceNotFoundException("Device not found: " + id));

        // Act + Assert
        mockMvc.perform(get("/devices/{id}", id))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Device not found: " + id));
    }

    @Test
    void getDeviceById_invalidUuid_returns400() throws Exception {
        mockMvc.perform(get("/devices/{id}", "uuid-invalido"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid format for id"));
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
        doThrow(new DeviceNotFoundException("Device not found: " + id))
            .when(deviceService).deleteDevice(id);

        mockMvc.perform(delete("/devices/{id}", id))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Device not found: " + id));
    }

    @Test
    void getDevicesByOwner_returnsList() throws Exception {
        UUID ownerId = UUID.randomUUID();
        DeviceResponse first = DeviceResponse.builder()
            .id(UUID.randomUUID())
            .ownerId(ownerId)
            .deviceName("Display North")
            .createdAt(LocalDateTime.now().minusDays(1))
            .updatedAt(LocalDateTime.now())
            .build();

        DeviceResponse second = DeviceResponse.builder()
            .id(UUID.randomUUID())
            .ownerId(ownerId)
            .deviceName("Display South")
            .createdAt(LocalDateTime.now().minusHours(8))
            .updatedAt(LocalDateTime.now())
            .build();

        when(deviceService.getDevicesByOwner(ownerId)).thenReturn(List.of(first, second));

        mockMvc.perform(get("/devices/owner/{ownerId}", ownerId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].deviceName").value("Display North"))
            .andExpect(jsonPath("$[1].deviceName").value("Display South"));
    }

    @Test
    void getDevicesByOwner_invalidUuid_returns400() throws Exception {
        mockMvc.perform(get("/devices/owner/{ownerId}", "uuid-invalido"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid format for ownerId"));
    }
}
