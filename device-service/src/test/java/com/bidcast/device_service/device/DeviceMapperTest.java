package com.bidcast.device_service.device;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.bidcast.device_service.device.dto.CreateDeviceRequest;
import com.bidcast.device_service.device.dto.DeviceResponse;

class DeviceMapperTest {

    @Test
    void fromCreateRequest_mapsFields() {
        // Arrange: request con datos validos.
        UUID ownerId = UUID.randomUUID();
        CreateDeviceRequest request = CreateDeviceRequest.builder()
            .ownerId(ownerId)
            .deviceName("Mi dispositivo")
            .build();

        // Act: mapeo a entidad.
        Device device = DeviceMapper.fromCreateRequest(request);

        // Assert: campos esperados.
        assertThat(device.getOwnerId()).isEqualTo(ownerId);
        assertThat(device.getDeviceName()).isEqualTo("Mi dispositivo");
    }

    @Test
    void toResponse_mapsFields() {
        // Arrange: entidad con timestamps.
        UUID id = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        LocalDateTime updatedAt = LocalDateTime.now();

        Device device = Device.builder()
            .id(id)
            .ownerId(ownerId)
            .deviceName("Sensor X")
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .build();

        // Act: mapeo a response DTO.
        DeviceResponse response = DeviceMapper.toResponse(device);

        // Assert: todos los campos copiados.
        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getOwnerId()).isEqualTo(ownerId);
        assertThat(response.getDeviceName()).isEqualTo("Sensor X");
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
        assertThat(response.getUpdatedAt()).isEqualTo(updatedAt);
    }
}
