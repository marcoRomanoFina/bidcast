package com.bidcast.session_service.session;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
    name = "sessions",
    indexes = {
        @Index(name = "idx_session_venue_status", columnList = "venue_id, status"),
        @Index(name = "idx_session_owner_status", columnList = "owner_id, status")
    }
)
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "venue_id", nullable = false, updatable = false)
    @NotNull(message = "Venue id is required")
    @Setter(AccessLevel.NONE)
    private UUID venueId;

    @Column(nullable = false, updatable = false)
    @NotBlank(message = "Session name is required")
    @Setter(AccessLevel.NONE)
    private String name;

    @Column(name = "owner_id", nullable = false, updatable = false)
    @NotNull(message = "Owner id is required")
    @Setter(AccessLevel.NONE)
    private UUID ownerId;

    @Column(name = "base_price_per_slot", nullable = false, precision = 19, scale = 4, updatable = false)
    @NotNull(message = "Base price per slot is required")
    @Positive(message = "Base price per slot must be positive")
    @Setter(AccessLevel.NONE)
    private BigDecimal basePricePerSlot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Session status is required")
    @Builder.Default
    private SessionStatus status = SessionStatus.WAITING_DEVICE;

    @OneToMany(mappedBy = "session", orphanRemoval = true)
    @Builder.Default
    private List<SessionDevice> devices = new ArrayList<>();

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "closed_reason")
    private SessionClosedReason closedReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    @Setter(AccessLevel.NONE)
    private Instant updatedAt;

    @Version
    @Setter(AccessLevel.NONE)
    private Long version;

    
    public void activate() {
        if (status != SessionStatus.WAITING_DEVICE) {
            throw new IllegalStateException("Only waiting sessions can be activated");
        }
        status = SessionStatus.ACTIVE;
        startedAt = Instant.now();
    }

    
    public void close() {
        close(SessionClosedReason.MANUAL);
    }

    public void close(SessionClosedReason reason) {
        if (status == SessionStatus.CLOSED) {
            throw new IllegalStateException("Session is already closed");
        }
        status = SessionStatus.CLOSED;
        endedAt = Instant.now();
        closedReason = reason;
    }

    public boolean isActive() {
        return status == SessionStatus.ACTIVE;
    }

    public boolean isWaitingForDevice() {
        return status == SessionStatus.WAITING_DEVICE;
    }

    public void waitForDevice() {
        if (status != SessionStatus.ACTIVE) {
            throw new IllegalStateException("Only active sessions can return to waiting for device");
        }
        status = SessionStatus.WAITING_DEVICE;
    }
}
