package com.bidcast.auction_service.bid;

import com.bidcast.auction_service.client.WalletClient;
import com.bidcast.auction_service.core.exception.SessionInactiveException;
import com.bidcast.auction_service.core.exception.WalletCommunicationException;
import com.bidcast.auction_service.bid.BidRehydrationService;
import com.bidcast.auction_service.session.SessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BidRegistrationServiceTest {

    @Mock private SessionService sessionService;
    @Mock private BidPersistenceService persistenceService;
    @Mock private WalletClient walletClient;
    @Mock private BidInfrastructureService infrastructureService;
    @Mock private BidRehydrationService rehydrationService;

    @InjectMocks
    private BidRegistrationService registrationService;

    private BidRegistrationRequest request;
    private SessionBid pendingBid;
    private SessionBid activeBid;

    @BeforeEach
    void setUp() {
        request = new BidRegistrationRequest(
                "session-1", "adv-1", "camp-1", 
                new BigDecimal("10.00"), new BigDecimal("0.50"), "url-1"
        );
        pendingBid = SessionBid.builder()
                .id(UUID.randomUUID())
                .sessionId("session-1")
                .advertiserId("adv-1")
                .totalBudget(new BigDecimal("10.00"))
                .status(BidStatus.PENDING_RESERVATION)
                .build();
        
        activeBid = SessionBid.builder()
                .id(pendingBid.getId())
                .sessionId("session-1")
                .advertiserId("adv-1")
                .totalBudget(new BigDecimal("10.00"))
                .status(BidStatus.ACTIVE)
                .build();
    }

    @Test
    @DisplayName("Registro exitoso: Flujo completo hasta Redis")
    void registerBid_Success() {
        when(sessionService.isSessionActive("session-1")).thenReturn(true);
        when(persistenceService.saveAsPending(any())).thenReturn(pendingBid);
        when(persistenceService.activate(any())).thenReturn(activeBid);
        when(rehydrationService.calculateRealBalanceCents(any(SessionBid.class))).thenReturn(1000L);

        SessionBid result = registrationService.registerBid(request);

        assertNotNull(result);
        assertEquals(BidStatus.ACTIVE, result.getStatus());
        verify(walletClient).freeze(any());
        verify(infrastructureService).injectIntoRedis(any(), eq(1000L));
        verify(persistenceService, never()).fail(any());
    }

    @Test
    @DisplayName("Falla si la sesión no está activa")
    void registerBid_FailsIfSessionInactive() {
        when(sessionService.isSessionActive("session-1")).thenReturn(false);

        assertThrows(SessionInactiveException.class, () -> registrationService.registerBid(request));
        verifyNoInteractions(persistenceService, walletClient);
    }

    @Test
    @DisplayName("Falla si Wallet Service no responde: Marca como FAILED")
    void registerBid_FailsOnWalletError() {
        when(sessionService.isSessionActive("session-1")).thenReturn(true);
        when(persistenceService.saveAsPending(any())).thenReturn(pendingBid);
        doThrow(new RuntimeException("Network Error")).when(walletClient).freeze(any());

        assertThrows(WalletCommunicationException.class, () -> registrationService.registerBid(request));
        
        verify(persistenceService).fail(pendingBid.getId());
        verifyNoInteractions(infrastructureService);
    }

    @Test
    @DisplayName("Compensación: Si falla la inyección en Redis, ejecuta unfreeze")
    void registerBid_TriggersCompensationOnRedisFailure() {
        when(sessionService.isSessionActive("session-1")).thenReturn(true);
        when(persistenceService.saveAsPending(any())).thenReturn(pendingBid);
        when(persistenceService.activate(any())).thenReturn(activeBid);
        
        // Simular fallo en la inyección a Redis
        doThrow(new RuntimeException("Redis Down")).when(infrastructureService).injectIntoRedis(any(), anyLong());

        assertThrows(RuntimeException.class, () -> registrationService.registerBid(request));

        // Verificar que se llamó al unfreeze
        verify(walletClient).unfreeze(any());
        // Verificar que se marcó como FAILED al final
        verify(persistenceService).fail(pendingBid.getId());
    }
}
