package com.bidcast.auction_service.settlement;

import com.bidcast.auction_service.bid.BidInfrastructureService;
import com.bidcast.auction_service.bid.BidPersistenceService;
import com.bidcast.auction_service.bid.BidRehydrationService;
import com.bidcast.auction_service.bid.BidStatus;
import com.bidcast.auction_service.bid.SessionBid;
import com.bidcast.auction_service.bid.SessionBidRepository;
import com.bidcast.auction_service.core.outbox.OutboxEvent;
import com.bidcast.auction_service.core.outbox.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

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
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private OutboxRepository outboxRepository;
    
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

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
    @DisplayName("Liquidación Exitosa: Genera evento Outbox con el gasto real")
    void orchestrateSettlement_Success() throws Exception {
        when(sessionBidRepository.findBySessionIdAndStatus(sessionId, BidStatus.ACTIVE)).thenReturn(List.of(activeBid));
        // Quedan $4.00 (400 centavos), gastó $6.00
        when(rehydrationService.calculateRealBalanceCents(any(SessionBid.class))).thenReturn(400L);

        orchestrator.orchestrateSettlement(sessionId, pubId);

        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository).save(eventCaptor.capture());
        
        OutboxEvent captured = eventCaptor.getValue();
        SessionSettlementCommand cmd = objectMapper.readValue(captured.getPayload(), SessionSettlementCommand.class);
        
        assertEquals(0, new BigDecimal("6.00").compareTo(cmd.totalSpent()));
        verify(persistenceService).close(activeBid.getId());
    }

    @Test
    @DisplayName("Sin Gasto: No genera evento Outbox si no se consumió nada")
    void orchestrateSettlement_NoSpendNoEvent() {
        when(sessionBidRepository.findBySessionIdAndStatus(sessionId, BidStatus.ACTIVE)).thenReturn(List.of(activeBid));
        // Quedan $10.00 (1000 centavos), gastó $0.00
        when(rehydrationService.calculateRealBalanceCents(any(SessionBid.class))).thenReturn(1000L);

        orchestrator.orchestrateSettlement(sessionId, pubId);

        verify(outboxRepository, never()).save(any());
        verify(persistenceService).close(activeBid.getId());
    }

    @Test
    @DisplayName("Error en liquidación: Lanza RuntimeException para disparar rollback")
    void orchestrateSettlement_ThrowsErrorOnFailure() {
        when(sessionBidRepository.findBySessionIdAndStatus(sessionId, BidStatus.ACTIVE)).thenReturn(List.of(activeBid));
        when(rehydrationService.calculateRealBalanceCents(any(SessionBid.class))).thenReturn(500L);
        doThrow(new RuntimeException("DB Error")).when(outboxRepository).save(any());

        assertThrows(RuntimeException.class, () -> orchestrator.orchestrateSettlement(sessionId, pubId));
    }

    @Test
    @DisplayName("Idempotencia: si el evento de settlement ya existe, no falla ni duplica")
    void orchestrateSettlement_IgnoresDuplicateOutboxEvent() {
        when(sessionBidRepository.findBySessionIdAndStatus(sessionId, BidStatus.ACTIVE)).thenReturn(List.of(activeBid));
        when(rehydrationService.calculateRealBalanceCents(any(SessionBid.class))).thenReturn(400L);
        doThrow(new DataIntegrityViolationException("duplicate"))
                .when(outboxRepository).save(any());

        assertDoesNotThrow(() -> orchestrator.orchestrateSettlement(sessionId, pubId));

        verify(persistenceService).close(activeBid.getId());
    }
}
