package com.bidcast.auction_service.auction;

import com.bidcast.auction_service.BaseIntegrationTest;
import com.bidcast.auction_service.bid.BidInfrastructureService;
import com.bidcast.auction_service.bid.BidMetadata;
import com.bidcast.auction_service.bid.BidRehydrationService;
import com.bidcast.auction_service.core.exception.SessionConcurrencyException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Import(BaseIntegrationTest.RedissonTestConfig.class)
class AuctionConcurrencyIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private AuctionEngine auctionEngine;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @MockitoBean
    private BidRehydrationService rehydrationService;

    @MockitoBean
    private BidInfrastructureService infrastructureService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("CONCURRENCIA (Integration): Dos hilos no pueden subastar la misma sesión simultáneamente")
    void shouldPreventConcurrentAuctionsOnSameSession() throws Exception {
        String sessionId = "session-concurrent-test";
        UUID bidId = UUID.randomUUID();
        
        // Setup: Ponemos datos en Redis para que no intente rehidratar (evita locks internos extras)
        String sessionSetKey = "session:" + sessionId + ":active_bids";
        String bidKey = "session:" + sessionId + ":bid:" + bidId;
        
        BidMetadata meta = new BidMetadata(bidId, "adv-1", "camp-1", new BigDecimal("1.50"), "url-1");
        
        redisTemplate.opsForSet().add(sessionSetKey, bidId.toString());
        redisTemplate.opsForHash().put(bidKey, "metadata", objectMapper.writeValueAsString(meta));
        redisTemplate.opsForHash().put(bidKey, "budget", "500");

        // Mockeamos el delay para "estirar" el tiempo que el primer hilo mantiene el lock
        doAnswer(invocation -> {
            Thread.sleep(1000); // Mantenemos el lock ocupado por 1s
            return null;
        }).when(infrastructureService).extendTTL(eq(sessionId), any());

        // Lanzamos dos hilos al mismo tiempo
        CompletableFuture<WinningAd> future1 = CompletableFuture.supplyAsync(() -> auctionEngine.evaluateNext(sessionId));
        
        // Esperamos un poquito para asegurar que el hilo 1 ya agarró el lock
        Thread.sleep(100);
        
        CompletableFuture<WinningAd> future2 = CompletableFuture.supplyAsync(() -> auctionEngine.evaluateNext(sessionId));

        // Resultados
        try {
            WinningAd winner = future1.get(5, TimeUnit.SECONDS);
            assertNotNull(winner);
            assertEquals(bidId, winner.bidId());
        } catch (Exception e) {
            fail("El primer hilo debió ganar la subasta: " + e.getMessage());
        }

        // El segundo hilo debió fallar por concurrencia
        ExecutionException ex = assertThrows(ExecutionException.class, () -> future2.get(5, TimeUnit.SECONDS));
        assertTrue(ex.getCause() instanceof SessionConcurrencyException, 
                "Se esperaba SessionConcurrencyException pero fue: " + ex.getCause().getClass().getName());
        
        System.out.println("Test de integración de concurrencia superado.");
    }
}
