package com.bidcast.selection_service.session;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "active_session_snapshots",
        indexes = {
                @Index(name = "idx_active_session_status_session_id", columnList = "status, session_id")
        }
)
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ActiveSessionSnapshot {

    @Id
    @Column(nullable = false, updatable = false)
    @NotBlank(message = "Session id is required")
    private String sessionId;

    @Column(nullable = false)
    @NotBlank(message = "Venue id is required")
    private String venueId;

    @Column(nullable = false)
    @NotBlank(message = "Owner id is required")
    private String ownerId;

    @Column(name = "base_price_per_slot", nullable = false, precision = 19, scale = 4)
    @NotNull(message = "Base price per slot is required")
    private BigDecimal basePricePerSlot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Session status is required")
    @Builder.Default
    private ActiveSessionStatus status = ActiveSessionStatus.ACTIVE;

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

    public void refresh(String venueId, String ownerId, BigDecimal basePricePerSlot) {
        this.venueId = venueId;
        this.ownerId = ownerId;
        this.basePricePerSlot = basePricePerSlot;
        this.status = ActiveSessionStatus.ACTIVE;
    }

    public void close() {
        this.status = ActiveSessionStatus.CLOSED;
    }
}
