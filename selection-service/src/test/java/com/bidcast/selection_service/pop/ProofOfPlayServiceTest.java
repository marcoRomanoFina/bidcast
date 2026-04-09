package com.bidcast.selection_service.pop;

import com.bidcast.selection_service.offer.OfferInfrastructureService;
import com.bidcast.selection_service.offer.SessionOfferRepository;
import com.bidcast.selection_service.core.exception.InvalidPlayReceiptException;
import com.bidcast.selection_service.receipt.ReceiptTokenService;
import com.bidcast.selection_service.receipt.ValidatedReceipt;
import org.springframework.dao.DataIntegrityViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import com.bidcast.selection_service.offer.OfferStatus;
import com.bidcast.selection_service.offer.CreativeSnapshot;
import com.bidcast.selection_service.offer.SessionOffer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProofOfPlayServiceTest {

    @Mock private ReceiptTokenService receiptTokenService;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private ProofOfPlayRepository proofOfPlayRepository;
    @Mock private OfferInfrastructureService infrastructureService;
    @Mock private SessionOfferRepository sessionOfferRepository;

    @InjectMocks
    private ProofOfPlayService proofOfPlayService;

    private PopRequest request;
    private UUID offerId = UUID.randomUUID();
    private String sessionId = "session-1";
    private String deviceId = "device-1";
    private String creativeId = "creative-1";
    private String receiptId = "valid-receipt";

    @BeforeEach
    void setUp() {
        request = new PopRequest(sessionId, deviceId, offerId.toString(), creativeId, receiptId);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(sessionOfferRepository.findById(any())).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("PoP exitoso: persiste, marca recibo y registra recencia")
    void recordPlay_Success() {
        // Arrange
        BigDecimal price = new BigDecimal("0.50");
        ValidatedReceipt validated = validatedReceipt(price, 1);
        SessionOffer bid = SessionOffer.builder()
                .id(offerId)
                .sessionId(sessionId)
                .advertiserId("adv-1")
                .campaignId("campaign-1")
                .totalBudget(new BigDecimal("100.00"))
                .pricePerSlot(new BigDecimal("0.50"))
                .deviceCooldownSeconds(300)
                .creatives(java.util.List.of(new CreativeSnapshot("creative-1", "https://cdn/1.mp4", 1)))
                .status(OfferStatus.ACTIVE)
                .build();
        
        when(receiptTokenService.validateAndExtract(anyString(), anyString(), any(), anyString(), anyLong())).thenReturn(validated);
        when(sessionOfferRepository.findById(offerId)).thenReturn(Optional.of(bid));
        // Idempotencia: no procesado aún
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        // Act
        proofOfPlayService.recordPlay(request);

        // Assert
        verify(proofOfPlayRepository).save(any());
        verify(valueOperations).set(eq("pop:processed:" + receiptId), eq("processed"), any(Duration.class));
        verify(infrastructureService).extendTTL(anyString(), anyList());
        verify(valueOperations).set(eq("session:" + sessionId + ":campaign:campaign-1:last_played"), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("Idempotencia: Ignora si el recibo ya fue procesado")
    void recordPlay_Idempotent() {
        // Arrange
        ValidatedReceipt validated = validatedReceipt(BigDecimal.ONE, 1);
        lenient().when(receiptTokenService.validateAndExtract(anyString(), anyString(), any(), anyString(), anyLong())).thenReturn(validated);
        
        // El recibo YA existe en Redis
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);

        // Act
        proofOfPlayService.recordPlay(request);

        // Assert
        verify(proofOfPlayRepository, never()).save(any());
    }

    @Test
    @DisplayName("Idempotencia persistente: Si el recibo ya está en DB, no vuelve a cobrar")
    void recordPlay_DatabaseDuplicateSkipsCharge() {
        ValidatedReceipt validated = validatedReceipt(BigDecimal.ONE, 1);
        when(receiptTokenService.validateAndExtract(anyString(), anyString(), any(), anyString(), anyLong())).thenReturn(validated);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(proofOfPlayRepository.save(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        proofOfPlayService.recordPlay(request);

        verify(infrastructureService, never()).extendTTL(anyString(), anyList());
        verify(valueOperations).set(eq("pop:processed:" + receiptId), eq("processed"), any(Duration.class));
    }

    @Test
    @DisplayName("Falla si el ticket es inválido")
    void recordPlay_InvalidTicket() {
        when(receiptTokenService.validateAndExtract(anyString(), anyString(), any(), anyString(), anyLong()))
            .thenThrow(new RuntimeException("Firma inválida"));

        assertThrows(InvalidPlayReceiptException.class, () -> proofOfPlayService.recordPlay(request));
        
        verifyNoInteractions(proofOfPlayRepository);
    }

    @Test
    @DisplayName("Orden crítico: Persiste el PoP antes de marcar el receipt procesado")
    void recordPlay_PersistsBeforeBudgetMutation() {
        ValidatedReceipt validated = validatedReceipt(new BigDecimal("0.50"), 1);
        when(receiptTokenService.validateAndExtract(anyString(), anyString(), any(), anyString(), anyLong())).thenReturn(validated);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        proofOfPlayService.recordPlay(request);

        InOrder inOrder = inOrder(proofOfPlayRepository, valueOperations);
        inOrder.verify(proofOfPlayRepository).save(any());
        inOrder.verify(valueOperations).set(eq("pop:processed:" + receiptId), eq("processed"), any(Duration.class));
    }

    @Test
    @DisplayName("PoP registra recencia global por campaign en Redis")
    void recordPlay_RegistersCampaignRecency() {
        ValidatedReceipt validated = validatedReceipt(new BigDecimal("0.50"), 1);
        SessionOffer bid = SessionOffer.builder()
                .id(offerId)
                .sessionId(sessionId)
                .advertiserId("adv-1")
                .campaignId("campaign-1")
                .totalBudget(new BigDecimal("100.00"))
                .pricePerSlot(new BigDecimal("0.50"))
                .deviceCooldownSeconds(300)
                .creatives(java.util.List.of(new CreativeSnapshot("creative-1", "https://cdn/1.mp4", 1)))
                .status(OfferStatus.ACTIVE)
                .build();

        when(receiptTokenService.validateAndExtract(anyString(), anyString(), any(), anyString(), anyLong())).thenReturn(validated);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        when(sessionOfferRepository.findById(offerId)).thenReturn(Optional.of(bid));

        proofOfPlayService.recordPlay(request);

        verify(valueOperations).set(eq("session:" + sessionId + ":campaign:campaign-1:last_played"), anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("PoP persiste el total según precio por slot y duración del creative")
    void recordPlay_PersistsTotalPriceBySlotCount() {
        ValidatedReceipt validated = validatedReceipt(new BigDecimal("0.50"), 3);
        when(receiptTokenService.validateAndExtract(anyString(), anyString(), any(), anyString(), anyLong())).thenReturn(validated);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);

        proofOfPlayService.recordPlay(request);

        verify(proofOfPlayRepository).save(argThat(pop -> pop.getCostCharged().compareTo(new BigDecimal("1.50")) == 0));
    }

    private ValidatedReceipt validatedReceipt(BigDecimal pricePerSlot, int slotCount) {
        return new ValidatedReceipt(
                "adv-1",
                creativeId,
                slotCount,
                pricePerSlot,
                pricePerSlot.multiply(BigDecimal.valueOf(slotCount))
        );
    }
}
