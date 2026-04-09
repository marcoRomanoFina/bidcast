package com.bidcast.wallet_service.event;

import com.bidcast.wallet_service.charge.SessionSettlementService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SettlementEventListenerTest {

    @Mock
    private SessionSettlementService sessionSettlementService;

    @InjectMocks
    private SettlementEventListener listener;

    @Test
    void handleSessionSettled_delegatesToService() {
        SessionSettledEvent event = new SessionSettledEvent(
                UUID.randomUUID(),
                Instant.now(),
                "offer-1",
                "session-1",
                "advertiser-1",
                "publisher-1",
                new BigDecimal("25.00"),
                new BigDecimal("40.00")
        );

        listener.handleSessionSettled(event);

        verify(sessionSettlementService).processSettlement(event);
    }
}
