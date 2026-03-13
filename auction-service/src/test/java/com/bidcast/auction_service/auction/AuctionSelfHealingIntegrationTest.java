package com.bidcast.auction_service.auction;

import com.bidcast.auction_service.TestcontainersConfiguration;
import com.bidcast.auction_service.bid.BidStatus;
import com.bidcast.auction_service.bid.SessionBid;
import com.bidcast.auction_service.bid.SessionBidRepository;
import com.bidcast.auction_service.session.DeviceSession;
import com.bidcast.auction_service.session.DeviceSessionRepository;
import com.bidcast.auction_service.session.SessionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Testcontainers
class AuctionSelfHealingIntegrationTest {

    @Autowired
    private AuctionEngine auctionEngine;

    @Autowired
    private SessionBidRepository bidRepository;

    @Autowired
    private DeviceSessionRepository sessionRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private String sessionId;
    private SessionBid nikeBid;

    @BeforeEach
    void setUp() {
        sessionId = "session-" + UUID.randomUUID();
        
        DeviceSession session = DeviceSession.builder()
                .sessionId(sessionId)
                .deviceId("device-heal")
                .publisherId("pub-1")
                .status(SessionStatus.ACTIVE)
                .build();
        sessionRepository.save(session);

        nikeBid = SessionBid.builder()
                .sessionId(sessionId)
                .advertiserId("nike")
                .campaignId("camp-nike")
                .totalBudget(BigDecimal.valueOf(100.00))
                .advertiserBidPrice(BigDecimal.valueOf(1.50))
                .mediaUrl("nike-url")
                .status(BidStatus.ACTIVE)
                .build();
        nikeBid = bidRepository.save(nikeBid);
    }

    @Test
    @DisplayName("SELF-HEAL: Amnesia total -> Masiva")
    void shouldHealWhenRedisIsEmpty() {
        String sessionSetKey = String.format("session:%s:active_bids", sessionId);
        redisTemplate.delete(sessionSetKey);

        AuctionResult result = auctionEngine.evaluateNext(sessionId);

        assertTrue(result instanceof WinningAd);
        assertEquals(nikeBid.getId(), ((WinningAd) result).bidId());
        assertTrue(redisTemplate.hasKey(sessionSetKey));
    }

    @Test
    @DisplayName("SELF-HEAL: Metadata faltante -> Quirúrgica")
    void shouldHealWhenMetadataIsMissing() {
        String bidId = nikeBid.getId().toString();
        String sessionSetKey = String.format("session:%s:active_bids", sessionId);
        String metadataKey = String.format("session:%s:bid:%s:metadata", sessionId, bidId);
        String budgetKey = String.format("session:%s:bid:%s:budget", sessionId, bidId);

        redisTemplate.opsForSet().add(sessionSetKey, bidId);
        redisTemplate.opsForValue().set(budgetKey, "10000");
        redisTemplate.delete(metadataKey);

        AuctionResult result = auctionEngine.evaluateNext(sessionId);

        assertTrue(result instanceof WinningAd);
        assertEquals(nikeBid.getId(), ((WinningAd) result).bidId());
        assertTrue(redisTemplate.hasKey(metadataKey));
    }
}
