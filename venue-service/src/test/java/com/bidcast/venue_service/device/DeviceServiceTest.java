package com.bidcast.venue_service.device;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bidcast.venue_service.device.dto.CreateDeviceRequest;
import com.bidcast.venue_service.device.dto.DeviceResponse;
import com.bidcast.venue_service.exception.ResourceNotFoundException;
import com.bidcast.venue_service.venue.Venue;
import com.bidcast.venue_service.venue.VenueService;

class DeviceServiceTest {

    private DeviceRepository deviceRepository;
    private VenueService venueService;
    private DeviceService deviceService;

    @BeforeEach
    void setUp() {
        deviceRepository = mock(DeviceRepository.class);
        venueService = mock(VenueService.class);
        deviceService = new DeviceService(deviceRepository, venueService);
    }

    @Test
    void createDevice_savesAndReturns() {
        // Arrange
        UUID venueId = UUID.randomUUID();
        Venue venue = Venue.builder().id(venueId).name("Venue Test").build();
        CreateDeviceRequest request = CreateDeviceRequest.builder()
            .venueId(venueId)
            .deviceName("Tracker")
            .build();

        Device saved = Device.builder()
            .id(UUID.randomUUID())
            .venue(venue)
            .deviceName("Tracker")
            .build();

        when(venueService.getVenueEntityById(venueId)).thenReturn(venue);
        when(deviceRepository.save(any(Device.class))).thenReturn(saved);

        // Act
        DeviceResponse result = deviceService.createDevice(request);

        // Assert
        assertThat(result.getId()).isEqualTo(saved.getId());
        assertThat(result.getVenueId()).isEqualTo(venueId);
        assertThat(result.getDeviceName()).isEqualTo("Tracker");
        verify(deviceRepository).save(any(Device.class));
    }

    @Test
    void getDeviceById_returnsDeviceWhenFound() {
        // Arrange
        UUID id = UUID.randomUUID();
        UUID venueId = UUID.randomUUID();
        Venue venue = Venue.builder().id(venueId).build();
        Device device = Device.builder()
            .id(id)
            .venue(venue)
            .deviceName("Sensor")
            .build();

        when(deviceRepository.findById(id)).thenReturn(Optional.of(device));

        // Act
        DeviceResponse result = deviceService.getDeviceById(id);

        // Assert
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getVenueId()).isEqualTo(venueId);
        assertThat(result.getDeviceName()).isEqualTo("Sensor");
        verify(deviceRepository).findById(id);
    }

    @Test
    void getDeviceById_throwsWhenNotFound() {
        // Arrange
        UUID id = UUID.randomUUID();
        when(deviceRepository.findById(id)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> deviceService.getDeviceById(id))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining(id.toString());
    }

    @Test
    void deleteDevice_deletesWhenExists() {
        // Arrange
        UUID id = UUID.randomUUID();
        when(deviceRepository.existsById(id)).thenReturn(true);

        // Act
        deviceService.deleteDevice(id);

        // Assert
        verify(deviceRepository).deleteById(id);
    }

    @Test
    void deleteDevice_throwsWhenNotExists() {
        // Arrange
        UUID id = UUID.randomUUID();
        when(deviceRepository.existsById(id)).thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() -> deviceService.deleteDevice(id))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining(id.toString());
    }

    @Test
    void getDevicesByVenue_returnsMappedList() {
        UUID venueId = UUID.randomUUID();
        Venue venue = Venue.builder().id(venueId).build();

        Device first = Device.builder()
            .id(UUID.randomUUID())
            .venue(venue)
            .deviceName("Screen A")
            .build();

        Device second = Device.builder()
            .id(UUID.randomUUID())
            .venue(venue)
            .deviceName("Screen B")
            .build();

        when(deviceRepository.findByVenueId(venueId)).thenReturn(List.of(first, second));

        List<DeviceResponse> result = deviceService.getDevicesByVenue(venueId);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(DeviceResponse::getDeviceName)
            .containsExactly("Screen A", "Screen B");
        verify(deviceRepository).findByVenueId(venueId);
    }
}
