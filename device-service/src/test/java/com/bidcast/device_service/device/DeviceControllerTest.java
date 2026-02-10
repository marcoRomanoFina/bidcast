package com.bidcast.device_service.device;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
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
            .andExpect(jsonPath("$.message").value("el nombre debe tener entre 3 y 100 caracteres"));
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
            .thenThrow(new DeviceNotFoundException("Dispositivo no encontrado: " + id));

        // Act + Assert
        mockMvc.perform(get("/devices/{id}", id))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").value("Dispositivo no encontrado: " + id));
    }
}
