package com.bidcast.auction_service.session;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.AccessLevel;
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
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DeviceSession {

    @Id
    @NotBlank(message = "Session id must not be blank")
    @Setter(AccessLevel.NONE)
    private String sessionId;

    @Column(nullable = false)
    @NotBlank(message = "Device id is required")
    @Setter(AccessLevel.NONE)
    private String deviceId;

    @Column(nullable = false)
    @NotBlank(message = "Publisher id is required")
    @Setter(AccessLevel.NONE)
    private String publisherId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Session status is required")
    @Setter(AccessLevel.NONE)
    private SessionStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Instant startedAt;

    @Setter(AccessLevel.NONE)
    private Instant closedAt;

    /**
     * Cierra la sesión de forma controlada.
     */
    public void close() {
        if (this.status == SessionStatus.CLOSED) {
            return; // Idempotencia
        }
        this.status = SessionStatus.CLOSED;
        this.closedAt = Instant.now();
    }
}
