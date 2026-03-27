package com.bidcast.device_service.device;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.bidcast.device_service.device.dto.CreateDeviceRequest;
import com.bidcast.device_service.device.dto.DeviceResponse;
import com.bidcast.device_service.exception.DeviceNotFoundException;

class DeviceServiceTest {

    private DeviceRepository deviceRepository;
    private DeviceService deviceService;

    @BeforeEach
    void setUp() {
        // Arrange: repo mockeado y service real.
        deviceRepository = Mockito.mock(DeviceRepository.class);
        deviceService = new DeviceService(deviceRepository);
    }

    @Test
    void createDevice_savesAndReturns() {
        // Arrange: request y entidad que devuelve el repo.
        UUID ownerId = UUID.randomUUID();
        CreateDeviceRequest request = CreateDeviceRequest.builder()
            .ownerId(ownerId)
            .deviceName("Tracker")
            .build();

        Device saved = Device.builder()
            .id(UUID.randomUUID())
            .ownerId(ownerId)
            .deviceName("Tracker")
            .build();

        when(deviceRepository.save(Mockito.any(Device.class))).thenReturn(saved);

        // Act: guardar desde el service.
        DeviceResponse result = deviceService.createDevice(request);

        // Assert: se devuelve el mismo objeto y se llama al repo.
        assertThat(result.getId()).isEqualTo(saved.getId());
        assertThat(result.getOwnerId()).isEqualTo(ownerId);
        assertThat(result.getDeviceName()).isEqualTo("Tracker");
        verify(deviceRepository).save(Mockito.any(Device.class));
    }

    @Test
    void getDeviceById_returnsDeviceWhenFound() {
        // Arrange: repo devuelve entidad existente.
        UUID id = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Device device = Device.builder()
            .id(id)
            .ownerId(ownerId)
            .deviceName("Sensor")
            .build();

        when(deviceRepository.findById(id)).thenReturn(Optional.of(device));

        // Act
        DeviceResponse result = deviceService.getDeviceById(id);

        // Assert
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getOwnerId()).isEqualTo(ownerId);
        assertThat(result.getDeviceName()).isEqualTo("Sensor");
        verify(deviceRepository).findById(id);
    }

    @Test
    void getDeviceById_throwsWhenNotFound() {
        // Arrange: repo devuelve vacío.
        UUID id = UUID.randomUUID();
        when(deviceRepository.findById(id)).thenReturn(Optional.empty());

        // Act + Assert: se lanza exception custom.
        assertThatThrownBy(() -> deviceService.getDeviceById(id))
            .isInstanceOf(DeviceNotFoundException.class)
            .hasMessageContaining(id.toString());
    }

    @Test
    void deleteDevice_deletesWhenExists() {
        // Arrange: el dispositivo existe.
        UUID id = UUID.randomUUID();
        when(deviceRepository.existsById(id)).thenReturn(true);

        // Act
        deviceService.deleteDevice(id);

        // Assert
        verify(deviceRepository).deleteById(id);
    }

    @Test
    void deleteDevice_throwsWhenNotExists() {
        // Arrange: el dispositivo no existe.
        UUID id = UUID.randomUUID();
        when(deviceRepository.existsById(id)).thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() -> deviceService.deleteDevice(id))
            .isInstanceOf(DeviceNotFoundException.class)
            .hasMessageContaining(id.toString());
    }

    @Test
    void getDevicesByOwner_returnsMappedList() {
        UUID ownerId = UUID.randomUUID();

        Device first = Device.builder()
            .id(UUID.randomUUID())
            .ownerId(ownerId)
            .deviceName("Screen A")
            .build();

        Device second = Device.builder()
            .id(UUID.randomUUID())
            .ownerId(ownerId)
            .deviceName("Screen B")
            .build();

        when(deviceRepository.findByOwnerId(ownerId)).thenReturn(List.of(first, second));

        List<DeviceResponse> result = deviceService.getDevicesByOwner(ownerId);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(DeviceResponse::getDeviceName)
            .containsExactly("Screen A", "Screen B");
        verify(deviceRepository).findByOwnerId(ownerId);
    }
}
