package com.bidcast.selection_service.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.bidcast.selection_service.SelectionServiceApplication;
import com.bidcast.selection_service.offer.CreativeSnapshot;
import com.bidcast.selection_service.offer.OfferInfrastructureService;
import com.bidcast.selection_service.offer.OfferStatus;
import com.bidcast.selection_service.offer.SessionOffer;
import com.bidcast.selection_service.offer.SessionOfferRepository;
import com.bidcast.selection_service.pop.PopRequest;
import com.bidcast.selection_service.pop.ProofOfPlayRepository;
import com.bidcast.selection_service.pop.ProofOfPlayService;
import com.bidcast.selection_service.selection.CandidateSelectionRequest;
import com.bidcast.selection_service.selection.SelectedCandidate;
import com.bidcast.selection_service.selection.SelectionCandidateService;

@SpringBootTest(classes = SelectionServiceApplication.class)
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class SelectionFlowIntegrationTest {

    private static final String TEST_REDIS_PASSWORD = "password";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
            .withCommand("redis-server", "--requirepass", TEST_REDIS_PASSWORD)
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> TEST_REDIS_PASSWORD);
        registry.add("spring.redisson.password", () -> TEST_REDIS_PASSWORD);
        registry.add("spring.task.scheduling.enabled", () -> "false");
    }

    @Autowired
    private SessionOfferRepository sessionOfferRepository;

    @Autowired
    private OfferInfrastructureService offerInfrastructureService;

    @Autowired
    private SelectionCandidateService selectionCandidateService;

    @Autowired
    private ProofOfPlayService proofOfPlayService;

    @Autowired
    private ProofOfPlayRepository proofOfPlayRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void cleanState() {
        proofOfPlayRepository.deleteAll();
        sessionOfferRepository.deleteAll();
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
    }

    @Test
    void selectionAndPopFlow_worksAgainstRealPostgresAndRedis() {
        SessionOffer savedOffer = sessionOfferRepository.save(SessionOffer.builder()
                .sessionId("session-1")
                .advertiserId("advertiser-1")
                .campaignId("campaign-1")
                .totalBudget(new BigDecimal("20.00"))
                .pricePerSlot(new BigDecimal("2.50"))
                .deviceCooldownSeconds(300)
                .creatives(List.of(
                        new CreativeSnapshot("creative-1", "https://cdn.example.com/creative-1.mp4", 3),
                        new CreativeSnapshot("creative-2", "https://cdn.example.com/creative-2.mp4", 1)
                ))
                .status(OfferStatus.ACTIVE)
                .build());

        offerInfrastructureService.injectIntoRedis(savedOffer, 2_000L);

        List<SelectedCandidate> selected = selectionCandidateService.selectCandidates(
                new CandidateSelectionRequest("session-1", "device-1", 1, List.of())
        );

        assertEquals(1, selected.size());
        SelectedCandidate play = selected.getFirst();
        assertEquals(savedOffer.getId(), play.offerId());
        assertEquals("creative-1", play.creativeId());
        assertNotNull(play.playReceiptId());

        long remainingBudget = offerInfrastructureService
                .getBudgetCents("session-1", savedOffer.getId().toString())
                .orElseThrow();
        assertEquals(1_250L, remainingBudget);

        assertTrue(offerInfrastructureService.isCreativeBlockedForDevice("session-1", "device-1", "creative-1"));
        assertFalse(offerInfrastructureService.isCreativeBlockedForDevice("session-1", "device-1", "creative-2"));

        proofOfPlayService.recordPlay(new PopRequest(
                "session-1",
                "device-1",
                play.offerId().toString(),
                play.creativeId(),
                play.playReceiptId()
        ));

        assertEquals(1, proofOfPlayRepository.count());
        assertEquals(new BigDecimal("7.5000"), proofOfPlayRepository.sumCostByOfferId(play.offerId().toString()));

        String campaignRecencyKey = "session:session-1:campaign:campaign-1:last_played";
        assertNotNull(redisTemplate.opsForValue().get(campaignRecencyKey));

        proofOfPlayService.recordPlay(new PopRequest(
                "session-1",
                "device-1",
                play.offerId().toString(),
                play.creativeId(),
                play.playReceiptId()
        ));

        assertEquals(1, proofOfPlayRepository.count());
    }
}
