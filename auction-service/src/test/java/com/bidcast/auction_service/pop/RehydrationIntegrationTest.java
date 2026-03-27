package com.bidcast.auction_service.pop;

import com.bidcast.auction_service.BaseIntegrationTest;
import com.bidcast.auction_service.bid.BidStatus;
import com.bidcast.auction_service.bid.SessionBid;
import com.bidcast.auction_service.bid.SessionBidRepository;
import com.bidcast.auction_service.auction.ReceiptTokenService;
import com.bidcast.auction_service.session.DeviceSession;
import com.bidcast.auction_service.session.DeviceSessionRepository;
import com.bidcast.auction_service.session.SessionStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class RehydrationIntegrationTest extends BaseIntegrationTest {

    @Autowired private ProofOfPlayService proofOfPlayService;
    @Autowired private SessionBidRepository bidRepository;
    @Autowired private ProofOfPlayRepository popRepository;
    @Autowired private DeviceSessionRepository sessionRepository;
    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private ReceiptTokenService tokenService;
    @Autowired private ObjectMapper objectMapper;

     
    private String sessionId;
    private SessionBid activeBid;

    @BeforeEach
    void setUp() {
        sessionId = "session-" + UUID.randomUUID();
        
        DeviceSession session = DeviceSession.builder()
                .sessionId(sessionId)
                .deviceId("device-1")
                .publisherId("pub-1")
                .status(SessionStatus.ACTIVE)
                .build();
        sessionRepository.save(session);

        activeBid = SessionBid.builder()
                .sessionId(sessionId)
                .advertiserId("adv-1")
                .campaignId("camp-1")
                .totalBudget(BigDecimal.valueOf(100.00))
                .advertiserBidPrice(BigDecimal.valueOf(10.00))
                .mediaUrl("url-1")
                .status(BidStatus.ACTIVE)
                .build();
        activeBid = bidRepository.save(activeBid);
    }

    @Test
    @DisplayName("REHIDRATACIÓN: Si Redis se borra, debe reconstruir el saldo desde los tickets de Postgres")
    void shouldRehydrateWhenRedisDataIsLost() throws Exception {
        for (int i = 0; i < 3; i++) {
            ProofOfPlay ticket = ProofOfPlay.builder()
                    .sessionId(sessionId)
                    .bidId(activeBid.getId().toString())
                    .advertiserId("adv-1")
                    .costCharged(BigDecimal.valueOf(10.00))
                    .playReceiptId("old-receipt-" + i)
                    .playedAt(Instant.now())
                    .build();
            popRepository.save(ticket);
        }

        String bidId = activeBid.getId().toString();
        String bidKey = String.format("session:%s:bid:%s", sessionId, bidId);
        String sessionSetKey = String.format("session:%s:active_bids", sessionId);
        
        redisTemplate.opsForHash().put(bidKey, "metadata", objectMapper.writeValueAsString(com.bidcast.auction_service.bid.BidMetadata.fromEntity(activeBid)));
        redisTemplate.opsForSet().add(sessionSetKey, bidId);

        // Borramos el Hash completo o solo el budget para forzar la rehidratación
        redisTemplate.opsForHash().delete(bidKey, "budget");

        String receiptId = tokenService.generateReceiptId(sessionId, activeBid.getId(), "adv-1", BigDecimal.valueOf(10.00));
        PopRequest request = new PopRequest(sessionId, activeBid.getId().toString(), receiptId);
        
        proofOfPlayService.recordPlay(request);

        String remainingCents = (String) redisTemplate.opsForHash().get(bidKey, "budget");
        
        assertNotNull(remainingCents);
        assertEquals("6000", remainingCents);
    }
}
