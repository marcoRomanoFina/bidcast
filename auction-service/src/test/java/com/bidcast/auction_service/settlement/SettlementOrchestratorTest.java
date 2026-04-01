package com.bidcast.auction_service.settlement;

import com.bidcast.auction_service.bid.BidInfrastructureService;
import com.bidcast.auction_service.bid.BidPersistenceService;
import com.bidcast.auction_service.bid.BidRehydrationService;
import com.bidcast.auction_service.bid.BidStatus;
import com.bidcast.auction_service.bid.SessionBid;
import com.bidcast.auction_service.bid.SessionBidRepository;
import com.bidcast.auction_service.core.event.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SettlementOrchestratorTest {

    @Mock private BidPersistenceService persistenceService;
    @Mock private SessionBidRepository sessionBidRepository;
    @Mock private BidInfrastructureService infrastructureService;
    @Mock private BidRehydrationService rehydrationService;
    @Mock private EventPublisher eventPublisher;

    @InjectMocks
    private SettlementOrchestrator orchestrator;

    private final String sessionId = "session-1";
    private final String pubId = "pub-1";
    private SessionBid activeBid;

    @BeforeEach
    void setUp() {
        activeBid = SessionBid.builder()
                .id(UUID.randomUUID())
                .sessionId(sessionId)
                .advertiserId("adv-1")
                .totalBudget(new BigDecimal("10.00"))
                .status(BidStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Liquidación Exitosa: Publica evento de dominio con el gasto real")
    void orchestrateSettlement_Success() {
        when(sessionBidRepository.findBySessionIdAndStatus(sessionId, BidStatus.ACTIVE)).thenReturn(List.of(activeBid));
        // Quedan $4.00 (400 centavos), gastó $6.00
        when(rehydrationService.calculateRealBalanceCents(any(SessionBid.class))).thenReturn(400L);

        orchestrator.orchestrateSettlement(sessionId, pubId);

        ArgumentCaptor<SessionSettledEvent> eventCaptor = ArgumentCaptor.forClass(SessionSettledEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        
        SessionSettledEvent published = eventCaptor.getValue();
        assertEquals(0, new BigDecimal("6.00").compareTo(published.totalSpent()));
        assertEquals(activeBid.getId().toString(), published.bidId());
        
        verify(persistenceService).close(activeBid.getId());
    }

    @Test
    @DisplayName("Sin Gasto: No publica evento si no se consumió nada")
    void orchestrateSettlement_NoSpendNoEvent() {
        when(sessionBidRepository.findBySessionIdAndStatus(sessionId, BidStatus.ACTIVE)).thenReturn(List.of(activeBid));
        // Quedan $10.00 (1000 centavos), gastó $0.00
        when(rehydrationService.calculateRealBalanceCents(any(SessionBid.class))).thenReturn(1000L);

        orchestrator.orchestrateSettlement(sessionId, pubId);

        verify(eventPublisher, never()).publish(any());
        verify(persistenceService).close(activeBid.getId());
    }

    @Test
    @DisplayName("Error en liquidación: Lanza RuntimeException para disparar rollback")
    void orchestrateSettlement_ThrowsErrorOnFailure() {
        when(sessionBidRepository.findBySessionIdAndStatus(sessionId, BidStatus.ACTIVE)).thenReturn(List.of(activeBid));
        when(rehydrationService.calculateRealBalanceCents(any(SessionBid.class))).thenReturn(500L);
        
        // Simular fallo en la publicación del evento
        doThrow(new RuntimeException("Persistence Error")).when(eventPublisher).publish(any());

        assertThrows(RuntimeException.class, () -> orchestrator.orchestrateSettlement(sessionId, pubId));
    }
}
