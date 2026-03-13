package com.bidcast.auction_service.auction;

import com.bidcast.auction_service.bid.BidMetadata;
import com.bidcast.auction_service.bid.BidRehydrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuctionEngineTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private SetOperations<String, String> setOperations;
    @Mock
    private ReceiptTokenService tokenService;
    @Mock
    private BidRehydrationService rehydrationService;
    
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AuctionEngine auctionEngine;

    private final String sessionId = "session-test";

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
    }

    @Test
    @DisplayName("Debe elegir la puja más alta con saldo suficiente")
    void shouldSelectHighestBidWithBudget() throws Exception {
        BidMetadata lowBid = createMetadata(BigDecimal.valueOf(10), "bid-low");
        BidMetadata highBid = createMetadata(BigDecimal.valueOf(20), "bid-high");
        
        String lowJson = objectMapper.writeValueAsString(lowBid);
        String highJson = objectMapper.writeValueAsString(highBid);

        when(setOperations.members(anyString())).thenReturn(Set.of(lowBid.id().toString(), highBid.id().toString()));
        
        when(valueOperations.multiGet(anyList())).thenReturn(
            Arrays.asList(lowJson, highJson),
            Arrays.asList("10000", "10000")
        );

        when(tokenService.generateReceiptId(anyString(), any(), anyString(), any())).thenReturn("mock-token");

        AuctionResult result = auctionEngine.evaluateNext(sessionId);

        assertTrue(result instanceof WinningAd);
        assertEquals(highBid.id(), ((WinningAd) result).bidId());
    }

    @Test
    @DisplayName("Debe devolver NoAdFound si nadie tiene saldo")
    void shouldReturnNoAdFoundWhenNoBudget() throws Exception {
        BidMetadata bid = createMetadata(BigDecimal.valueOf(50), "bid-high");
        String bidJson = objectMapper.writeValueAsString(bid);

        when(setOperations.members(anyString())).thenReturn(Set.of(bid.id().toString()));
        
        when(valueOperations.multiGet(anyList())).thenReturn(
            List.of(bidJson),
            List.of("100")
        );

        AuctionResult result = auctionEngine.evaluateNext(sessionId);

        assertTrue(result instanceof NoAdFound);
    }

    private BidMetadata createMetadata(BigDecimal price, String name) {
        return new BidMetadata(
                UUID.randomUUID(),
                "adv-" + name,
                "camp-" + name,
                price,
                name + "-url"
        );
    }
}
