package com.bidcast.wallet_service.event;

import com.bidcast.wallet_service.charge.SessionSettlementService;
import com.bidcast.wallet_service.charge.dto.SessionSettlementCommand;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SettlementEventListenerTest {

    @Mock
    private SessionSettlementService sessionSettlementService;

    @InjectMocks
    private SettlementEventListener listener;

    @Test
    void handleSessionSettlement_delegatesToService() {
        SessionSettlementCommand command = new SessionSettlementCommand(
                "bid-1",
                "session-1",
                "advertiser-1",
                "publisher-1",
                new BigDecimal("25.00"),
                new BigDecimal("40.00")
        );

        listener.handleSessionSettlement(command);

        verify(sessionSettlementService).processSettlement(command);
    }
}
