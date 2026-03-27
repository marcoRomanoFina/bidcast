package com.bidcast.auction_service.pop;

import com.bidcast.auction_service.bid.BidInfrastructureService;
import com.bidcast.auction_service.bid.BidPersistenceService;
import com.bidcast.auction_service.bid.BidRehydrationService;
import com.bidcast.auction_service.bid.BidStatus;
import com.bidcast.auction_service.bid.RestoredBid;
import com.bidcast.auction_service.auction.ReceiptTokenService;
import com.bidcast.auction_service.auction.ValidatedReceipt;
import com.bidcast.auction_service.core.exception.InvalidPlayReceiptException;
import org.springframework.dao.DataIntegrityViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProofOfPlayServiceTest {

    @Mock private ReceiptTokenService receiptTokenService;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private HashOperations<String, Object, Object> hashOperations;
    @Mock private ProofOfPlayRepository proofOfPlayRepository;
    @Mock private BidPersistenceService persistenceService;
    @Mock private BidInfrastructureService infrastructureService;
    @Mock private BidRehydrationService rehydrationService;

    @InjectMocks
    private ProofOfPlayService proofOfPlayService;

    private PopRequest request;
    private UUID bidId = UUID.randomUUID();
    private String sessionId = "session-1";
    private String receiptId = "valid-receipt";

    @BeforeEach
    void setUp() {
        request = new PopRequest(sessionId, bidId.toString(), receiptId);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    @Test
    @DisplayName("PoP Exitoso: Descuenta presupuesto y persiste")
    void recordPlay_Success() {
        // Arrange
        BigDecimal price = new BigDecimal("0.50");
        ValidatedReceipt validated = new ValidatedReceipt("adv-1", price);
        
        when(receiptTokenService.validateAndExtract(anyString(), anyString(), any(), anyLong())).thenReturn(validated);
        // Idempotencia: no procesado aún
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        // Descuento en Redis: queda saldo (50 centavos)
        when(hashOperations.increment(anyString(), eq("budget"), eq(-50L))).thenReturn(50L);

        // Act
        proofOfPlayService.recordPlay(request);

        // Assert
        verify(hashOperations).increment(anyString(), eq("budget"), eq(-50L));
        verify(proofOfPlayRepository).save(any());
        verify(valueOperations).set(eq("pop:processed:" + receiptId), eq("processed"), any(Duration.class));
        verify(infrastructureService).extendTTL(anyString(), anyList());
    }

    @Test
    @DisplayName("Idempotencia: Ignora si el recibo ya fue procesado")
    void recordPlay_Idempotent() {
        // Arrange
        ValidatedReceipt validated = new ValidatedReceipt("adv-1", BigDecimal.ONE);
        lenient().when(receiptTokenService.validateAndExtract(anyString(), anyString(), any(), anyLong())).thenReturn(validated);
        
        // El recibo YA existe en Redis
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        // Act
        proofOfPlayService.recordPlay(request);

        // Assert
        verify(hashOperations, never()).increment(anyString(), anyString(), anyLong());
        verify(proofOfPlayRepository, never()).save(any());
    }

    @Test
    @DisplayName("Idempotencia persistente: Si el recibo ya está en DB, no vuelve a cobrar")
    void recordPlay_DatabaseDuplicateSkipsCharge() {
        ValidatedReceipt validated = new ValidatedReceipt("adv-1", BigDecimal.ONE);
        when(receiptTokenService.validateAndExtract(anyString(), anyString(), any(), anyLong())).thenReturn(validated);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(proofOfPlayRepository.save(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        proofOfPlayService.recordPlay(request);

        verify(hashOperations, never()).increment(anyString(), anyString(), anyLong());
        verify(infrastructureService, never()).extendTTL(anyString(), anyList());
        verify(valueOperations).set(eq("pop:processed:" + receiptId), eq("processed"), any(Duration.class));
    }

    @Test
    @DisplayName("Falla si el ticket es inválido")
    void recordPlay_InvalidTicket() {
        when(receiptTokenService.validateAndExtract(anyString(), anyString(), any(), anyLong()))
            .thenThrow(new RuntimeException("Firma inválida"));

        assertThrows(InvalidPlayReceiptException.class, () -> proofOfPlayService.recordPlay(request));
        
        verifyNoInteractions(proofOfPlayRepository);
    }

    @Test
    @DisplayName("Auto-sanación: Si el presupuesto en Redis falla, rehidrata")
    void recordPlay_TriggersRehydrationIfBudgetMissing() {
        ValidatedReceipt validated = new ValidatedReceipt("adv-1", new BigDecimal("1.00"));
        when(receiptTokenService.validateAndExtract(anyString(), anyString(), any(), anyLong())).thenReturn(validated);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        
        // Simular que increment devuelve null (clave no existe en Redis)
        when(hashOperations.increment(anyString(), eq("budget"), anyLong())).thenReturn(null);
        
        RestoredBid restored = new RestoredBid(null, 500L);
        when(rehydrationService.rehydrateFullBid(bidId)).thenReturn(restored);

        proofOfPlayService.recordPlay(request);

        verify(rehydrationService).rehydrateFullBid(bidId);
        // El PoP ya fue persistido antes, el saldo 500 ya incluye el coste de 100.
    }

    @Test
    @DisplayName("Orden crítico: Persiste el PoP antes de tocar Redis")
    void recordPlay_PersistsBeforeBudgetMutation() {
        ValidatedReceipt validated = new ValidatedReceipt("adv-1", new BigDecimal("0.50"));
        when(receiptTokenService.validateAndExtract(anyString(), anyString(), any(), anyLong())).thenReturn(validated);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(hashOperations.increment(anyString(), eq("budget"), eq(-50L))).thenReturn(100L);

        proofOfPlayService.recordPlay(request);

        InOrder inOrder = inOrder(proofOfPlayRepository, hashOperations, valueOperations);
        inOrder.verify(proofOfPlayRepository).save(any());
        inOrder.verify(hashOperations).increment(anyString(), eq("budget"), eq(-50L));
        inOrder.verify(valueOperations).set(eq("pop:processed:" + receiptId), eq("processed"), any(Duration.class));
    }

    @Test
    @DisplayName("Kill-Switch: Remueve del índice si el presupuesto se agota")
    void recordPlay_RemovesFromIndexWhenExhausted() {
        ValidatedReceipt validated = new ValidatedReceipt("adv-1", new BigDecimal("0.50"));
        when(receiptTokenService.validateAndExtract(anyString(), anyString(), any(), anyLong())).thenReturn(validated);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        
        // El nuevo saldo es 40 centavos, y el coste es 50 -> se agota
        when(hashOperations.increment(anyString(), eq("budget"), eq(-50L))).thenReturn(40L);

        proofOfPlayService.recordPlay(request);

        verify(infrastructureService).removeFromActiveIndex(sessionId, bidId.toString());
        verify(persistenceService).updateStatus(bidId, BidStatus.EXHAUSTED);
    }
}
