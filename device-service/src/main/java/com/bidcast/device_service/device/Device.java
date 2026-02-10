package com.bidcast.device_service.device;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "Devices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    @NotNull(message = "el owner ID es obligatorio")
    private UUID ownerId;

    @Column(name = "device_name", nullable = false, length = 100)
    @NotBlank(message = "el nombre no puede estar vacio")
    @Size(min = 3, max = 100, message = "el nombre debe tener entre 3 y 100 caracteres")
    private String deviceName;

    @Column(name = "created_at", nullable = false, updatable = false) 
    @CreationTimestamp 
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp 
    private LocalDateTime updatedAt;
}
