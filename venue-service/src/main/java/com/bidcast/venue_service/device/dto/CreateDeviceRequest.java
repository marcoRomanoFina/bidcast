package com.bidcast.venue_service.device.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateDeviceRequest {

    @NotNull(message = "Venue ID is required")
    private UUID venueId;

    @NotBlank(message = "Device name must not be blank")
    @Size(min = 3, max = 100, message = "Device name must be between 3 and 100 characters")
    private String deviceName;
}
