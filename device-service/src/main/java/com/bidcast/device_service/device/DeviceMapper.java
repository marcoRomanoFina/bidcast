package com.bidcast.device_service.device;

import com.bidcast.device_service.device.dto.CreateDeviceRequest;
import com.bidcast.device_service.device.dto.DeviceResponse;

public final class DeviceMapper {

    private DeviceMapper() {
    }

    public static Device fromCreateRequest(CreateDeviceRequest request) {
        return Device.builder()
            .ownerId(request.getOwnerId())
            .deviceName(request.getDeviceName())
            .build();
    }

    public static DeviceResponse toResponse(Device device) {
        return DeviceResponse.builder()
            .id(device.getId())
            .ownerId(device.getOwnerId())
            .deviceName(device.getDeviceName())
            .createdAt(device.getCreatedAt())
            .updatedAt(device.getUpdatedAt())
            .build();
    }
}
