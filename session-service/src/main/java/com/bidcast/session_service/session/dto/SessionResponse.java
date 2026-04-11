package com.bidcast.session_service.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.bidcast.session_service.session.Session;
import com.bidcast.session_service.session.SessionClosedReason;
import com.bidcast.session_service.session.SessionStatus;

public record SessionResponse(
        @Schema(description = "Session identifier")
        UUID id,
        @Schema(description = "Venue that owns the session")
        UUID venueId,
        @Schema(description = "Human-friendly session name")
        String name,
        @Schema(description = "Owner of the venue")
        UUID ownerId,
        @Schema(description = "Minimum accepted price per slot for the session")
        BigDecimal basePricePerSlot,
        @Schema(description = "Current lifecycle status of the session")
        SessionStatus status,
        @Schema(description = "Reason why the session was closed when applicable", nullable = true)
        SessionClosedReason closedReason,
        @Schema(description = "Timestamp when the session became active", nullable = true)
        Instant startedAt,
        @Schema(description = "Timestamp when the session was closed", nullable = true)
        Instant endedAt,
        @Schema(description = "Timestamp when the session was created")
        Instant createdAt
) {
    public static SessionResponse from(Session session) {
        return new SessionResponse(
                session.getId(),
                session.getVenueId(),
                session.getName(),
                session.getOwnerId(),
                session.getBasePricePerSlot(),
                session.getStatus(),
                session.getClosedReason(),
                session.getStartedAt(),
                session.getEndedAt(),
                session.getCreatedAt()
        );
    }
}
