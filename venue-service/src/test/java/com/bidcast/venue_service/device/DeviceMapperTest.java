package com.bidcast.venue_service.device;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.bidcast.venue_service.device.dto.CreateDeviceRequest;
import com.bidcast.venue_service.device.dto.DeviceResponse;
import com.bidcast.venue_service.venue.Venue;

class DeviceMapperTest {

    @Test
    void fromCreateRequest_mapsFields() {
        // Arrange: request con datos validos.
        UUID venueId = UUID.randomUUID();
        CreateDeviceRequest request = CreateDeviceRequest.builder()
            .venueId(venueId)
            .deviceName("Mi dispositivo")
            .build();

        // Act: mapeo a entidad.
        Device device = DeviceMapper.fromCreateRequest(request);

        // Assert: campos esperados.
        assertThat(device.getDeviceName()).isEqualTo("Mi dispositivo");
    }

    @Test
    void toResponse_mapsFields() {
        // Arrange: entidad con timestamps.
        UUID id = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();
        Venue venue = Venue.builder().id(venueId).build();
        LocalDateTime createdAt = LocalDateTime.now().minusDays(1);
        LocalDateTime updatedAt = LocalDateTime.now();

        Device device = Device.builder()
            .id(id)
            .venue(venue)
            .deviceName("Sensor X")
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .build();

        // Act: mapeo a response DTO.
        DeviceResponse response = DeviceMapper.toResponse(device);

        // Assert: todos los campos copiados.
        assertThat(response.getId()).isEqualTo(id);
        assertThat(response.getVenueId()).isEqualTo(venueId);
        assertThat(response.getDeviceName()).isEqualTo("Sensor X");
        assertThat(response.getCreatedAt()).isEqualTo(createdAt);
        assertThat(response.getUpdatedAt()).isEqualTo(updatedAt);
    }
}
