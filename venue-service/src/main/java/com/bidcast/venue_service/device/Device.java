package com.bidcast.venue_service.device;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.bidcast.venue_service.venue.Venue;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "devices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "venue")
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "device_name", nullable = false, length = 100)
    @NotBlank(message = "Device name must not be blank")
    @Size(min = 3, max = 100, message = "Device name must be between 3 and 100 characters")
    private String deviceName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id")
    @JsonIgnore
    private Venue venue;

    @Column(name = "created_at", nullable = false, updatable = false) 
    @CreationTimestamp 
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    @UpdateTimestamp 
    private LocalDateTime updatedAt;
}
