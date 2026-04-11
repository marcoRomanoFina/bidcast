package com.bidcast.selection_service.session;

import java.time.Instant;

public record SessionClosedEvent(
        String sessionId,
        Instant closedAt
) {
}
