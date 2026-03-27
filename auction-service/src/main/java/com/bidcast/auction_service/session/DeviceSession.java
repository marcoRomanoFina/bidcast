package com.bidcast.auction_service.session;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;


// entidad que representa una session

@Entity
@Table(
    name = "device_sessions",
    indexes = {
        @Index(name = "idx_session_status_time", columnList = "status, startedAt")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceSession {

    @Id
    @NotBlank(message = "Session id must not be blank")
    private String sessionId;

    @Column(nullable = false)
    @NotBlank(message = "Device id is required")
    private String deviceId;

    @Column(nullable = false)
    @NotBlank(message = "Publisher id is required")
    private String publisherId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Session status is required")
    private SessionStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant startedAt;

    private Instant closedAt;
}
