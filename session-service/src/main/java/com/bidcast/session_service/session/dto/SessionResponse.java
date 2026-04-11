package com.bidcast.session_service.session.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.bidcast.session_service.session.Session;
import com.bidcast.session_service.session.SessionClosedReason;
import com.bidcast.session_service.session.SessionStatus;

public record SessionResponse(
        UUID id,
        UUID venueId,
        String name,
        UUID ownerId,
        BigDecimal basePricePerSlot,
        SessionStatus status,
        SessionClosedReason closedReason,
        Instant startedAt,
        Instant endedAt,
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
