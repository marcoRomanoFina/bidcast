package com.bidcast.session_service.session;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "session_devices",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_session_device", columnNames = {"session_id", "device_id"})
        },
        indexes = {
                @Index(name = "idx_session_device_lookup", columnList = "session_id, device_id"),
                @Index(name = "idx_session_device_session_status", columnList = "session_id, status")
        }
)
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SessionDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false, updatable = false)
    @NotNull(message = "Session is required")
    private Session session;

    @Column(name = "device_id", nullable = false, updatable = false)
    @NotNull(message = "Device id is required")
    private UUID deviceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Device status is required")
    @Builder.Default
    private SessionDeviceStatus status = SessionDeviceStatus.READY;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Instant joinedAt;

    public void markReady() {
        status = SessionDeviceStatus.READY;
        lastSeenAt = Instant.now();
    }

    public void recordHeartbeat() {
        if (status == SessionDeviceStatus.DISCONNECTED) {
            status = SessionDeviceStatus.READY;
        }
        lastSeenAt = Instant.now();
    }

    public void leave() {
        status = SessionDeviceStatus.LEFT;
        lastSeenAt = Instant.now();
    }

    public void markDisconnected() {
        status = SessionDeviceStatus.DISCONNECTED;
    }
}
