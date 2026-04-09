package com.bidcast.venue_service.device;

import com.bidcast.venue_service.device.dto.CreateDeviceRequest;
import com.bidcast.venue_service.device.dto.DeviceResponse;

public class DeviceMapper {

    public static Device fromCreateRequest(CreateDeviceRequest request) {
        return Device.builder()
                .deviceName(request.getDeviceName())
                .build();
    }

    public static DeviceResponse toResponse(Device device) {
        return DeviceResponse.builder()
                .id(device.getId())
                .venueId(device.getVenue() != null ? device.getVenue().getId() : null)
                .deviceName(device.getDeviceName())
                .createdAt(device.getCreatedAt())
                .updatedAt(device.getUpdatedAt())
                .build();
    }
}
