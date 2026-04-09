package com.bidcast.venue_service.device.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceResponse {
    private UUID id;
    private UUID venueId;
    private String deviceName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
