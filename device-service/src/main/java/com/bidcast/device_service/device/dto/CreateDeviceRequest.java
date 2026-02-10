package com.bidcast.device_service.device.dto;

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

    @NotNull(message = "el owner ID es obligatorio")
    private UUID ownerId;

    @NotBlank(message = "el nombre no puede estar vacio")
    @Size(min = 3, max = 100, message = "el nombre debe tener entre 3 y 100 caracteres")
    private String deviceName;
}
