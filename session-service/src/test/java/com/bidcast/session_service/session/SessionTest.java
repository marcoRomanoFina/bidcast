package com.bidcast.session_service.session;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionTest {

    @Test
    void activate_marksSessionAsActiveAndStoresStartTime() {
        Session session = Session.builder()
                .venueId(UUID.randomUUID())
                .name("Lunch rush session")
                .ownerId(UUID.randomUUID())
                .basePricePerSlot(BigDecimal.valueOf(2.50))
                .build();

        session.activate();

        assertThat(session.getStatus()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(session.getStartedAt()).isNotNull();
        assertThat(session.isActive()).isTrue();
    }

    @Test
    void close_marksSessionAsClosedAndStoresEndTime() {
        Session session = Session.builder()
                .venueId(UUID.randomUUID())
                .name("Lunch rush session")
                .ownerId(UUID.randomUUID())
                .basePricePerSlot(BigDecimal.valueOf(2.50))
                .build();

        session.activate();
        session.close();

        assertThat(session.getStatus()).isEqualTo(SessionStatus.CLOSED);
        assertThat(session.getEndedAt()).isNotNull();
        assertThat(session.getClosedReason()).isEqualTo(SessionClosedReason.MANUAL);
        assertThat(session.isActive()).isFalse();
    }

    @Test
    void close_withReasonStoresSpecificClosedReason() {
        Session session = Session.builder()
                .venueId(UUID.randomUUID())
                .name("Lunch rush session")
                .ownerId(UUID.randomUUID())
                .basePricePerSlot(BigDecimal.valueOf(2.50))
                .build();

        session.activate();
        session.close(SessionClosedReason.NO_DEVICES);

        assertThat(session.getStatus()).isEqualTo(SessionStatus.CLOSED);
        assertThat(session.getClosedReason()).isEqualTo(SessionClosedReason.NO_DEVICES);
    }

    @Test
    void activate_rejectsInvalidTransition() {
        Session session = Session.builder()
                .venueId(UUID.randomUUID())
                .name("Lunch rush session")
                .ownerId(UUID.randomUUID())
                .basePricePerSlot(BigDecimal.valueOf(2.50))
                .status(SessionStatus.CLOSED)
                .build();

        assertThatThrownBy(session::activate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Only waiting sessions can be activated");
    }
}
