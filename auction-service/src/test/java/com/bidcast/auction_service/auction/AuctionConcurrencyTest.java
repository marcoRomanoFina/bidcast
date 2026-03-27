package com.bidcast.auction_service.auction;

import com.bidcast.auction_service.BaseIntegrationTest;
import com.bidcast.auction_service.bid.BidStatus;
import com.bidcast.auction_service.bid.SessionBid;
import com.bidcast.auction_service.bid.SessionBidRepository;
import com.bidcast.auction_service.core.outbox.OutboxRelay;
import com.bidcast.auction_service.core.outbox.OutboxRepository;
import com.bidcast.auction_service.settlement.SettlementOrchestrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuctionConcurrencyTest extends BaseIntegrationTest {

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @Autowired private AuctionEngine auctionEngine;
    @Autowired private SettlementOrchestrator settlementOrchestrator;
    @Autowired private OutboxRelay outboxRelay;
    @Autowired private OutboxRepository outboxRepository;
    @Autowired private SessionBidRepository sessionBidRepository;
    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private com.bidcast.auction_service.pop.ProofOfPlayRepository proofOfPlayRepository;
    @Autowired private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    private final String SESSION_ID = "test-session-123";
    private final String PUBLISHER_ID = "pub-456";

    @BeforeEach
    void cleanUp() {
        outboxRepository.deleteAll();
        proofOfPlayRepository.deleteAll();
        sessionBidRepository.deleteAll();
        redisTemplate.getRequiredConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    void outboxRelay_isThreadSafeWithSkipLocked() throws InterruptedException {
        // 1. Setup: Generamos 100 eventos de cobro en el Outbox
        for (int i = 0; i < 100; i++) {
            SessionBid bid = createTestBid(SESSION_ID + "-" + i, "adv-1", new BigDecimal("100.00"), new BigDecimal("1.50"));
            bid = sessionBidRepository.save(bid);
            
            // Registramos un PoP para que haya algo que liquidar (> 0)
            proofOfPlayRepository.save(com.bidcast.auction_service.pop.ProofOfPlay.builder()
                    .sessionId(bid.getSessionId())
                    .bidId(bid.getId().toString())
                    .advertiserId(bid.getAdvertiserId())
                    .costCharged(new BigDecimal("1.50"))
                    .playReceiptId("receipt-" + i)
                    .playedAt(java.time.Instant.now())
                    .build());

            settlementOrchestrator.orchestrateSettlement(bid.getSessionId(), PUBLISHER_ID);
        }

        long pendingBefore = outboxRepository.count();
        assertTrue(pendingBefore > 0, "Debería haber eventos en el outbox");

        // 2. Simular 10 hilos concurrentes
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);

        for (int i = 0; i < threadCount; i++) {
            executor.execute(() -> {
                try {
                    latch.await();
                    // Intentamos despachar un par de veces para asegurar que si el SKIP LOCKED
                    // nos saltó mensajes bloqueados por otros hilos, los agarremos después.
                    outboxRelay.scheduleDispatch();
                    Thread.sleep(100);
                    outboxRelay.scheduleDispatch();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        latch.countDown(); 
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        // 3. Verificación
        long pendingAfter = outboxRepository.findAll().stream()
                .filter(e -> !e.isProcessed())
                .count();

        assertEquals(0, pendingAfter, "Todos los cobros deben haber sido despachados");
    }

    @Test
    void auctionEngine_autoHealsWhenRedisIsEmpty() {
        SessionBid bid = createTestBid(SESSION_ID, "adv-1", new BigDecimal("100.00"), new BigDecimal("1.50"));
        bid = sessionBidRepository.save(bid);
        UUID bidId = bid.getId();

        WinningAd result = auctionEngine.evaluateNext(SESSION_ID);

        assertTrue(result instanceof WinningAd);
        WinningAd winner = (WinningAd) result;
        assertEquals(bidId, winner.bidId());
        assertTrue(redisTemplate.hasKey("session:" + SESSION_ID + ":active_bids"));
    }

    @Test
    void auctionEngine_filtersOutBidsWithInsufficientBudget() throws Exception {
        SessionBid richBid = sessionBidRepository.save(createTestBid(SESSION_ID, "rich-adv", new BigDecimal("10.00"), new BigDecimal("1.50")));
        SessionBid poorBid = sessionBidRepository.save(createTestBid(SESSION_ID, "poor-adv", new BigDecimal("0.01"), new BigDecimal("1.50")));

        String richKey = String.format("session:%s:bid:%s", SESSION_ID, richBid.getId());
        String poorKey = String.format("session:%s:bid:%s", SESSION_ID, poorBid.getId());

        com.bidcast.auction_service.bid.BidMetadata richMeta = com.bidcast.auction_service.bid.BidMetadata.fromEntity(richBid);
        com.bidcast.auction_service.bid.BidMetadata poorMeta = com.bidcast.auction_service.bid.BidMetadata.fromEntity(poorBid);

        redisTemplate.opsForHash().put(richKey, "budget", "1000");
        redisTemplate.opsForHash().put(richKey, "metadata", objectMapper.writeValueAsString(richMeta));
        
        redisTemplate.opsForHash().put(poorKey, "budget", "1");
        redisTemplate.opsForHash().put(poorKey, "metadata", objectMapper.writeValueAsString(poorMeta));

        redisTemplate.opsForSet().add("session:" + SESSION_ID + ":active_bids", richBid.getId().toString(), poorBid.getId().toString());

        WinningAd result = auctionEngine.evaluateNext(SESSION_ID);

        assertTrue(result instanceof WinningAd);
        assertEquals(richBid.getId(), ((WinningAd) result).bidId());
    }

    @Test
    void settlement_calculatesExactSpentAmount() {
        SessionBid bid = sessionBidRepository.save(createTestBid(SESSION_ID, "adv-1", new BigDecimal("100.00"), new BigDecimal("1.00")));
        
        // El settlement ahora usa PoPs en DB para calcular el gasto real
        proofOfPlayRepository.save(com.bidcast.auction_service.pop.ProofOfPlay.builder()
                .sessionId(SESSION_ID)
                .bidId(bid.getId().toString())
                .advertiserId(bid.getAdvertiserId())
                .costCharged(new BigDecimal("54.50"))
                .playReceiptId("receipt-123")
                .playedAt(java.time.Instant.now())
                .build());

        settlementOrchestrator.orchestrateSettlement(SESSION_ID, PUBLISHER_ID);

        var events = outboxRepository.findAll();
        assertTrue(events.size() > 0, "Debería haber un evento de liquidación");
        assertTrue(events.get(0).getPayload().contains("54.5"));
    }

    private SessionBid createTestBid(String sessionId, String advertiserId, BigDecimal totalBudget, BigDecimal bidPrice) {
        return SessionBid.builder()
                .sessionId(sessionId)
                .advertiserId(advertiserId)
                .campaignId("camp-test")
                .mediaUrl("http://media.com/test.jpg")
                .advertiserBidPrice(bidPrice)
                .totalBudget(totalBudget)
                .status(BidStatus.ACTIVE)
                .build();
    }
}
