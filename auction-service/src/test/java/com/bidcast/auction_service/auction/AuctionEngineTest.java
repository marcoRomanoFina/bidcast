package com.bidcast.auction_service.auction;

import com.bidcast.auction_service.bid.BidInfrastructureService;
import com.bidcast.auction_service.bid.BidMetadata;
import com.bidcast.auction_service.bid.BidRehydrationService;
import com.bidcast.auction_service.core.exception.AuctionExecutionException;
import com.bidcast.auction_service.core.exception.NoAdFoundException;
import com.bidcast.auction_service.core.exception.SessionConcurrencyException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuctionEngineTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock auctionLock;
    
    @Mock
    private RLock rehydrateLock;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private ReceiptTokenService receiptTokenService;

    @Mock
    private BidRehydrationService rehydrationService;

    @Mock
    private BidInfrastructureService infrastructureService;

    @Mock
    private AuctionPersistenceService auctionPersistenceService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @InjectMocks
    private AuctionEngine auctionEngine;

    private final String sessionId = "session-123";

    @BeforeEach
    void setUp() throws InterruptedException {
        doReturn(setOperations).when(redisTemplate).opsForSet();
        doReturn(valueOperations).when(redisTemplate).opsForValue();
        doReturn(hashOperations).when(redisTemplate).opsForHash();
        
        when(redissonClient.getLock(contains("lock:auction:"))).thenReturn(auctionLock);
        when(redissonClient.getLock(contains("lock:rehydrate:"))).thenReturn(rehydrateLock);
        
        when(auctionLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(auctionLock.isHeldByCurrentThread()).thenReturn(true);
        
        when(rehydrateLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(rehydrateLock.isHeldByCurrentThread()).thenReturn(true);
    }

    @Test
    @DisplayName("Gana el anuncio con mayor precio de puja y persiste")
    void evaluateNext_HighestPriceWinsAndPersists() throws Exception {
        // Arrange
        UUID bidId1 = UUID.randomUUID();
        UUID bidId2 = UUID.randomUUID();
        doReturn(Set.of(bidId1.toString(), bidId2.toString())).when(setOperations).members(anyString());

        BidMetadata meta1 = new BidMetadata(bidId1, "adv-1", "camp-1", new BigDecimal("0.50"), "url-1");
        BidMetadata meta2 = new BidMetadata(bidId2, "adv-2", "camp-2", new BigDecimal("0.80"), "url-2");

        List<Object> pipelineResults = List.of(
            List.of(objectMapper.writeValueAsString(meta1), "100"),
            List.of(objectMapper.writeValueAsString(meta2), "100")
        );
        when(redisTemplate.executePipelined(any(SessionCallback.class))).thenReturn(pipelineResults);

        doReturn("receipt-123").when(receiptTokenService).generateReceiptId(anyString(), any(), anyString(), any());

        // Act
        WinningAd winner = auctionEngine.evaluateNext(sessionId);

        // Assert
        assertNotNull(winner);
        assertEquals("adv-2", winner.advertiserId());
        verify(auctionPersistenceService).persistAuctionSuccess(any(WinningAd.class));
        verify(auctionLock).unlock();
    }

    @Test
    @DisplayName("CONCURRENCIA: Rebota si ya hay una subasta en curso para esa sesión (Lock)")
    void evaluateNext_RejectsConcurrentAuction() throws InterruptedException {
        when(auctionLock.tryLock(anyLong(), anyLong(), any(TimeUnit.class))).thenReturn(false);

        assertThrows(SessionConcurrencyException.class, () -> auctionEngine.evaluateNext(sessionId));
        
        verify(auctionPersistenceService, never()).persistAuctionSuccess(any());
        verify(auctionLock, never()).unlock();
    }

    @Test
    @DisplayName("Lanza NoAdFoundException si no hay pujas activas")
    void evaluateNext_NoBidsFound() throws InterruptedException {
        doReturn(Collections.emptySet()).when(setOperations).members(anyString());
        
        assertThrows(NoAdFoundException.class, () -> auctionEngine.evaluateNext(sessionId));
        
        verify(auctionPersistenceService, never()).persistAuctionSuccess(any());
        verify(auctionLock).unlock();
    }

    @Test
    @DisplayName("Lanza AuctionExecutionException si ocurre un error inesperado")
    void evaluateNext_UnexpectedError_ThrowsAuctionExecutionException() {
        doThrow(new RuntimeException("Redis connection failed")).when(setOperations).members(anyString());

        assertThrows(AuctionExecutionException.class, () -> auctionEngine.evaluateNext(sessionId));
        
        verify(auctionPersistenceService, never()).persistAuctionSuccess(any());
        verify(auctionLock).unlock();
    }
}
