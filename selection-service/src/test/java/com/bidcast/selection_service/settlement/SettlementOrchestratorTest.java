package com.bidcast.selection_service.settlement;

import com.bidcast.selection_service.offer.OfferInfrastructureService;
import com.bidcast.selection_service.offer.OfferPersistenceService;
import com.bidcast.selection_service.offer.OfferRehydrationService;
import com.bidcast.selection_service.offer.OfferStatus;
import com.bidcast.selection_service.offer.CreativeSnapshot;
import com.bidcast.selection_service.offer.SessionOffer;
import com.bidcast.selection_service.offer.SessionOfferRepository;
import com.bidcast.selection_service.core.event.EventPublisher;
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

    @Mock private OfferPersistenceService persistenceService;
    @Mock private SessionOfferRepository sessionOfferRepository;
    @Mock private OfferInfrastructureService infrastructureService;
    @Mock private OfferRehydrationService rehydrationService;
    @Mock private EventPublisher eventPublisher;

    @InjectMocks
    private SettlementOrchestrator orchestrator;

    private final String sessionId = "session-1";
    private final String pubId = "pub-1";
    private SessionOffer activeOffer;
    private SessionOffer exhaustedOffer;

    @BeforeEach
    void setUp() {
        activeOffer = SessionOffer.builder()
                .id(UUID.randomUUID())
                .sessionId(sessionId)
                .advertiserId("adv-1")
                .campaignId("camp-1")
                .totalBudget(new BigDecimal("10.00"))
                .pricePerSlot(new BigDecimal("2.00"))
                .deviceCooldownSeconds(300)
                .creatives(List.of(new CreativeSnapshot("creative-1", "url-1", 1)))
                .status(OfferStatus.ACTIVE)
                .build();
        exhaustedOffer = SessionOffer.builder()
                .id(UUID.randomUUID())
                .sessionId(sessionId)
                .advertiserId("adv-2")
                .campaignId("camp-2")
                .totalBudget(new BigDecimal("12.00"))
                .pricePerSlot(new BigDecimal("3.00"))
                .deviceCooldownSeconds(300)
                .creatives(List.of(new CreativeSnapshot("creative-2", "url-2", 1)))
                .status(OfferStatus.EXHAUSTED)
                .build();
    }

    @Test
    @DisplayName("Liquidación Exitosa: Publica evento de dominio con el gasto real")
    void orchestrateSettlement_Success() {
        when(sessionOfferRepository.findBySessionIdAndStatusIn(eq(sessionId), anyList())).thenReturn(List.of(activeOffer));
        // Quedan $4.00 (400 centavos), gastó $6.00
        when(rehydrationService.calculateRealBalanceCents(any(SessionOffer.class))).thenReturn(400L);

        orchestrator.orchestrateSettlement(sessionId, pubId);

        ArgumentCaptor<SessionSettledEvent> eventCaptor = ArgumentCaptor.forClass(SessionSettledEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        
        SessionSettledEvent published = eventCaptor.getValue();
        assertEquals(0, new BigDecimal("6.00").compareTo(published.totalSpent()));
        assertEquals(activeOffer.getId().toString(), published.offerId());
        
        verify(persistenceService).close(activeOffer.getId());
    }

    @Test
    @DisplayName("Sin Gasto: No publica evento si no se consumió nada")
    void orchestrateSettlement_NoSpendNoEvent() {
        when(sessionOfferRepository.findBySessionIdAndStatusIn(eq(sessionId), anyList())).thenReturn(List.of(activeOffer));
        // Quedan $10.00 (1000 centavos), gastó $0.00
        when(rehydrationService.calculateRealBalanceCents(any(SessionOffer.class))).thenReturn(1000L);

        orchestrator.orchestrateSettlement(sessionId, pubId);

        verify(eventPublisher, never()).publish(any());
        verify(persistenceService).close(activeOffer.getId());
    }

    @Test
    @DisplayName("Error en liquidación: Lanza RuntimeException para disparar rollback")
    void orchestrateSettlement_ThrowsErrorOnFailure() {
        when(sessionOfferRepository.findBySessionIdAndStatusIn(eq(sessionId), anyList())).thenReturn(List.of(activeOffer));
        when(rehydrationService.calculateRealBalanceCents(any(SessionOffer.class))).thenReturn(500L);
        
        // Simular fallo en la publicación del evento
        doThrow(new RuntimeException("Persistence Error")).when(eventPublisher).publish(any());

        assertThrows(RuntimeException.class, () -> orchestrator.orchestrateSettlement(sessionId, pubId));
    }

    @Test
    @DisplayName("Liquida también offers exhausted al cerrar la session")
    void orchestrateSettlement_IncludesExhaustedOffers() {
        when(sessionOfferRepository.findBySessionIdAndStatusIn(eq(sessionId), anyList()))
                .thenReturn(List.of(exhaustedOffer));
        when(rehydrationService.calculateRealBalanceCents(exhaustedOffer)).thenReturn(0L);

        orchestrator.orchestrateSettlement(sessionId, pubId);

        ArgumentCaptor<SessionSettledEvent> eventCaptor = ArgumentCaptor.forClass(SessionSettledEvent.class);
        verify(eventPublisher).publish(eventCaptor.capture());
        assertEquals(exhaustedOffer.getId().toString(), eventCaptor.getValue().offerId());
        verify(persistenceService).close(exhaustedOffer.getId());
    }
}
