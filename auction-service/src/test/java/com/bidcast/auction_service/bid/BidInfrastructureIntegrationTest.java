package com.bidcast.auction_service.bid;

import com.bidcast.auction_service.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Import(BaseIntegrationTest.RedissonTestConfig.class)
class BidInfrastructureIntegrationTest extends BaseIntegrationTest {

    private final BidInfrastructureService infrastructureService;
    private final StringRedisTemplate redisTemplate;

    @Autowired
    public BidInfrastructureIntegrationTest(BidInfrastructureService infrastructureService,
                                           StringRedisTemplate redisTemplate) {
        this.infrastructureService = infrastructureService;
        this.redisTemplate = redisTemplate;
    }

    private final String sessionId = "test-session";
    private SessionBid bid;

    @BeforeEach
    void setUp() {
        Set<String> keys = redisTemplate.keys("*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }

        bid = SessionBid.builder()
                .id(UUID.randomUUID())
                .sessionId(sessionId)
                .advertiserId("adv-123")
                .campaignId("camp-456")
                .advertiserBidPrice(new BigDecimal("0.75"))
                .mediaUrl("https://media.com/ad.mp4")
                .build();
    }

    @Test
    @DisplayName("Integración Redis: Inyectar anuncio en Hash y verificar persistencia e índice")
    void injectIntoRedis_Success() {
        infrastructureService.injectIntoRedis(bid, 1000L);

        String bidId = bid.getId().toString();
        String bidKey = String.format("session:%s:bid:%s", sessionId, bidId);

        assertTrue(redisTemplate.hasKey(bidKey));
        String metaJson = (String) redisTemplate.opsForHash().get(bidKey, "metadata");
        assertNotNull(metaJson);
        assertTrue(metaJson.contains("adv-123"));

        String budgetValue = (String) redisTemplate.opsForHash().get(bidKey, "budget");
        assertEquals("1000", budgetValue);

        String sessionSetKey = String.format("session:%s:active_bids", sessionId);
        Boolean isMember = redisTemplate.opsForSet().isMember(sessionSetKey, bidId);
        assertTrue(isMember);
    }

    @Test
    @DisplayName("Kill-Switch: Remover anuncio del índice cuando se agota")
    void removeFromActiveIndex_Success() {
        String bidId = bid.getId().toString();
        String sessionSetKey = String.format("session:%s:active_bids", sessionId);
        redisTemplate.opsForSet().add(sessionSetKey, bidId);
        
        infrastructureService.removeFromActiveIndex(sessionId, bidId);
        
        Boolean isMember = redisTemplate.opsForSet().isMember(sessionSetKey, bidId);
        assertFalse(isMember);
    }

    @Test
    @DisplayName("Limpieza de Sesión: Elimina el índice completo")
    void purgeSessionIndex_Success() {
        String sessionSetKey = String.format("session:%s:active_bids", sessionId);
        redisTemplate.opsForSet().add(sessionSetKey, UUID.randomUUID().toString());

        infrastructureService.purgeSessionIndex(sessionId);

        assertFalse(redisTemplate.hasKey(sessionSetKey));
    }
}
