package com.bidcast.auction_service.session;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

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
    @NotBlank(message = "El ID de sesión no puede estar vacío")
    private String sessionId;

    @Column(nullable = false)
    @NotBlank(message = "El ID del dispositivo es obligatorio")
    private String deviceId;

    @Column(nullable = false)
    @NotBlank(message = "El ID del publisher es obligatorio")
    private String publisherId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "El estado de la sesión es obligatorio")
    private SessionStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant startedAt;

    private Instant closedAt;
}
