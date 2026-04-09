package com.bidcast.selection_service.offer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class SessionOfferTest {

    @Test
    void advanceCreativePointer_rotatesCreativesInOrder() {
        SessionOffer bid = baseBid();

        assertEquals("creative-a", bid.advanceCreativePointer().orElseThrow().creativeId());
        assertEquals(1, bid.getNextCreativeIndex());

        assertEquals("creative-b", bid.advanceCreativePointer().orElseThrow().creativeId());
        assertEquals(2, bid.getNextCreativeIndex());

        assertEquals("creative-c", bid.advanceCreativePointer().orElseThrow().creativeId());
        assertEquals(0, bid.getNextCreativeIndex());
    }

    @Test
    void advanceToNextEligibleCreative_skipsExcludedCreativesAndAdvancesPointer() {
        SessionOffer bid = baseBid();

        CreativeSnapshot selected = bid.advanceToNextEligibleCreative(Set.of("creative-a", "creative-b"))
                .orElseThrow();

        assertEquals("creative-c", selected.creativeId());
        assertEquals(0, bid.getNextCreativeIndex());
    }

    @Test
    void advanceToNextEligibleCreative_returnsEmptyWhenEverythingIsExcluded() {
        SessionOffer bid = baseBid();

        assertTrue(bid.advanceToNextEligibleCreative(Set.of("creative-a", "creative-b", "creative-c")).isEmpty());
        assertEquals(0, bid.getNextCreativeIndex());
    }

    private SessionOffer baseBid() {
        return SessionOffer.builder()
                .sessionId("session-1")
                .advertiserId("advertiser-1")
                .campaignId("campaign-1")
                .totalBudget(new BigDecimal("100.00"))
                .pricePerSlot(new BigDecimal("5.00"))
                .deviceCooldownSeconds(300)
                .creatives(List.of(
                        new CreativeSnapshot("creative-a", "https://cdn/a.mp4", 1),
                        new CreativeSnapshot("creative-b", "https://cdn/b.mp4", 1),
                        new CreativeSnapshot("creative-c", "https://cdn/c.mp4", 1)
                ))
                .status(OfferStatus.ACTIVE)
                .build();
    }
}
