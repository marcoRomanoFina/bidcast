package com.bidcast.auction_service.pop;

import com.bidcast.auction_service.TestcontainersConfiguration;
import com.bidcast.auction_service.bid.BidStatus;
import com.bidcast.auction_service.bid.SessionBid;
import com.bidcast.auction_service.bid.SessionBidRepository;
import com.bidcast.auction_service.auction.ReceiptTokenService;
import com.bidcast.auction_service.session.DeviceSession;
import com.bidcast.auction_service.session.DeviceSessionRepository;
import com.bidcast.auction_service.session.SessionStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Testcontainers
class ProofOfPlayServiceIntegrationTest {

    @Autowired
    private ProofOfPlayService proofOfPlayService;

    @Autowired
    private SessionBidRepository bidRepository;

    @Autowired
    private DeviceSessionRepository sessionRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ReceiptTokenService tokenService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("CONCURRENCIA HORIZONTAL: 10 pantallas distintas cobrando al mismo tiempo")
    void testHorizontalConcurrency() throws Exception {
        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        List<SessionBid> createdBids = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            String sid = "session-" + i;
            
            DeviceSession session = DeviceSession.builder()
                    .sessionId(sid)
                    .deviceId("device-" + i)
                    .publisherId("pub-" + i)
                    .status(SessionStatus.ACTIVE)
                    .build();
            sessionRepository.save(session);

            SessionBid bid = SessionBid.builder()
                    .sessionId(sid)
                    .advertiserId("adv-" + i)
                    .campaignId("camp-" + i)
                    .totalBudget(BigDecimal.valueOf(1.00))
                    .advertiserBidPrice(BigDecimal.valueOf(0.10))
                    .mediaUrl("url-" + i)
                    .status(BidStatus.ACTIVE)
                    .build();
            bid = bidRepository.save(bid);
            createdBids.add(bid);

            String bidId = bid.getId().toString();
            String budgetKey = String.format("session:%s:bid:%s:budget", sid, bidId);
            String metadataKey = String.format("session:%s:bid:%s:metadata", sid, bidId);
            String sessionSetKey = String.format("session:%s:active_bids", sid);

            redisTemplate.opsForValue().set(budgetKey, "100");
            redisTemplate.opsForValue().set(metadataKey, objectMapper.writeValueAsString(bid));
            redisTemplate.opsForSet().add(sessionSetKey, bidId);
        }

        for (SessionBid bid : createdBids) {
            executor.execute(() -> {
                try {
                    String receiptId = tokenService.generateReceiptId(
                            bid.getSessionId(), 
                            bid.getId(), 
                            bid.getAdvertiserId(), 
                            bid.getAdvertiserBidPrice()
                    );
                    PopRequest request = new PopRequest(bid.getSessionId(), bid.getId().toString(), receiptId);
                    proofOfPlayService.recordPlay(request);
                } catch (Exception e) {
                    System.err.println("Error en hilo de test: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(15, TimeUnit.SECONDS);

        for (SessionBid bid : createdBids) {
            String budgetKey = String.format("session:%s:bid:%s:budget", bid.getSessionId(), bid.getId());
            String remainingCents = redisTemplate.opsForValue().get(budgetKey);
            assertEquals("90", remainingCents);
        }
    }
}
