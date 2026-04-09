package com.bidcast.selection_service.selection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import java.math.BigDecimal;
import java.util.List;
import java.util.OptionalLong;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.bidcast.selection_service.core.exception.SelectionInfrastructureUnavailableException;
import com.bidcast.selection_service.core.exception.SessionSelectionBusyException;
import com.bidcast.selection_service.offer.OfferStatus;
import com.bidcast.selection_service.offer.CreativeSnapshot;
import com.bidcast.selection_service.offer.SessionOffer;
import com.bidcast.selection_service.offer.SessionOfferRepository;
import com.bidcast.selection_service.receipt.ReceiptTokenService;

@ExtendWith(MockitoExtension.class)
class SelectionCandidateServiceTest {

    @Mock private SessionOfferRepository sessionOfferRepository;
    @Mock private SelectionLockService selectionLockService;
    @Mock private SelectionScoringService selectionScoringService;
    @Mock private SelectionReservationService selectionReservationService;
    @Mock private ReceiptTokenService receiptTokenService;

    @InjectMocks
    private SelectionCandidateService selectionCandidateService;

    @BeforeEach
    void setUp() {
        lenient().doAnswer(invocation -> invocation.<java.util.function.Supplier<?>>getArgument(1).get())
                .when(selectionLockService).withSessionLock(anyString(), any());
    }

    @Test
    void selectCandidates_returnsTopNCandidatesWithResolvedCreatives() {
        SessionOffer higherBid = bid(
                UUID.randomUUID(),
                "campaign-a",
                new BigDecimal("10.00"),
                List.of(
                        new CreativeSnapshot("creative-a1", "https://cdn/a1.mp4", 1),
                        new CreativeSnapshot("creative-a2", "https://cdn/a2.mp4", 1)
                )
        );
        SessionOffer lowerBid = bid(
                UUID.randomUUID(),
                "campaign-b",
                new BigDecimal("6.00"),
                List.of(List.of(new CreativeSnapshot("creative-b1", "https://cdn/b1.mp4", 1)).getFirst())
        );

        when(sessionOfferRepository.findBySessionIdAndStatus("session-1", OfferStatus.ACTIVE))
                .thenReturn(List.of(higherBid, lowerBid));
        when(selectionScoringService.candidateForOffer(eq(higherBid), any(), any(), any(), any()))
                .thenReturn(new SelectionCandidatePick(higherBid, higherBid.getCreatives().get(0), new BigDecimal("10.00")))
                .thenReturn(new SelectionCandidatePick(higherBid, higherBid.getCreatives().get(1), new BigDecimal("10.00")));
        when(selectionScoringService.candidateForOffer(eq(lowerBid), any(), any(), any(), any()))
                .thenReturn(new SelectionCandidatePick(lowerBid, lowerBid.getCreatives().get(0), new BigDecimal("6.00")))
                .thenReturn(SelectionCandidatePick.empty(lowerBid));
        when(selectionReservationService.consumeBudgetForSelection(eq(higherBid), any(), eq(higherBid.getCreatives().get(0)))).thenReturn(OptionalLong.of(9_000L));
        when(selectionReservationService.consumeBudgetForSelection(eq(higherBid), any(), eq(higherBid.getCreatives().get(1)))).thenReturn(OptionalLong.of(8_000L));
        when(receiptTokenService.generateReceiptId(eq("session-1"), any(), any(), any(), any(), any()))
                .thenReturn("receipt-1", "receipt-2");

        List<SelectedCandidate> result = selectionCandidateService.selectCandidates(
                new CandidateSelectionRequest("session-1", "device-1", 2, List.of())
        );

        assertEquals(2, result.size());
        assertEquals("creative-a1", result.get(0).creativeId());
        assertEquals("creative-a2", result.get(1).creativeId());
        assertEquals(300, result.get(0).deviceCooldownSeconds());
        verify(sessionOfferRepository).saveAll(any());
        verify(selectionReservationService).reserveCreativeForDevice(higherBid, new CandidateSelectionRequest("session-1", "device-1", 2, List.of()), higherBid.getCreatives().get(0));
        verify(selectionReservationService).reserveCreativeForDevice(higherBid, new CandidateSelectionRequest("session-1", "device-1", 2, List.of()), higherBid.getCreatives().get(1));
    }

    @Test
    void selectCandidates_skipsExcludedCreativesAndAppliesCampaignPenalty() {
        SessionOffer penalizedBid = bid(
                UUID.randomUUID(),
                "campaign-a",
                new BigDecimal("10.00"),
                List.of(new CreativeSnapshot("creative-a1", "https://cdn/a1.mp4", 1))
        );
        SessionOffer cleanBid = bid(
                UUID.randomUUID(),
                "campaign-b",
                new BigDecimal("9.00"),
                List.of(
                        new CreativeSnapshot("creative-b1", "https://cdn/b1.mp4", 1),
                        new CreativeSnapshot("creative-b2", "https://cdn/b2.mp4", 1)
                )
        );

        when(sessionOfferRepository.findBySessionIdAndStatus("session-1", OfferStatus.ACTIVE))
                .thenReturn(List.of(penalizedBid, cleanBid));
        when(selectionScoringService.candidateForOffer(eq(penalizedBid), any(), any(), any(), any()))
                .thenReturn(new SelectionCandidatePick(penalizedBid, penalizedBid.getCreatives().get(0), new BigDecimal("7.50")));
        when(selectionScoringService.candidateForOffer(eq(cleanBid), any(), any(), any(), any()))
                .thenReturn(new SelectionCandidatePick(cleanBid, cleanBid.getCreatives().get(1), new BigDecimal("9.00")));
        when(selectionReservationService.consumeBudgetForSelection(eq(cleanBid), any(), eq(cleanBid.getCreatives().get(1)))).thenReturn(OptionalLong.of(9_100L));
        when(receiptTokenService.generateReceiptId(eq("session-1"), any(), any(), any(), any(), any()))
                .thenReturn("receipt-1");

        List<SelectedCandidate> result = selectionCandidateService.selectCandidates(
                new CandidateSelectionRequest("session-1", "device-1", 1, List.of("creative-b1"))
        );

        assertEquals(1, result.size());
        assertEquals("campaign-b", result.getFirst().campaignId());
        assertEquals("creative-b2", result.getFirst().creativeId());
        verify(selectionReservationService).reserveCreativeForDevice(cleanBid, new CandidateSelectionRequest("session-1", "device-1", 1, List.of("creative-b1")), cleanBid.getCreatives().get(1));
    }

    @Test
    void selectCandidates_returnsEmptyWhenEveryCreativeIsExcluded() {
        SessionOffer bid = bid(
                UUID.randomUUID(),
                "campaign-a",
                new BigDecimal("10.00"),
                List.of(new CreativeSnapshot("creative-a1", "https://cdn/a1.mp4", 1))
        );

        when(sessionOfferRepository.findBySessionIdAndStatus("session-1", OfferStatus.ACTIVE))
                .thenReturn(List.of(bid));
        when(selectionScoringService.candidateForOffer(eq(bid), any(), any(), any(), any()))
                .thenReturn(SelectionCandidatePick.empty(bid));

        List<SelectedCandidate> result = selectionCandidateService.selectCandidates(
                new CandidateSelectionRequest("session-1", "device-1", 2, List.of("creative-a1"))
        );

        assertIterableEquals(List.of(), result);
    }

    @Test
    void selectCandidates_skipsCreativesBlockedInRedisForTheDevice() {
        SessionOffer offer = bid(
                UUID.randomUUID(),
                "campaign-a",
                new BigDecimal("10.00"),
                List.of(
                        new CreativeSnapshot("creative-a1", "https://cdn/a1.mp4", 1),
                        new CreativeSnapshot("creative-a2", "https://cdn/a2.mp4", 1)
                )
        );

        when(sessionOfferRepository.findBySessionIdAndStatus("session-1", OfferStatus.ACTIVE))
                .thenReturn(List.of(offer));
        when(selectionScoringService.candidateForOffer(eq(offer), any(), any(), any(), any()))
                .thenReturn(new SelectionCandidatePick(offer, offer.getCreatives().get(1), new BigDecimal("10.00")));
        when(selectionReservationService.consumeBudgetForSelection(eq(offer), any(), eq(offer.getCreatives().get(1)))).thenReturn(OptionalLong.of(9_000L));
        when(receiptTokenService.generateReceiptId(eq("session-1"), any(), any(), any(), any(), any()))
                .thenReturn("receipt-1");

        List<SelectedCandidate> result = selectionCandidateService.selectCandidates(
                new CandidateSelectionRequest("session-1", "device-1", 1, List.of())
        );

        assertEquals(1, result.size());
        assertEquals("creative-a2", result.getFirst().creativeId());
        verify(selectionReservationService).reserveCreativeForDevice(offer, new CandidateSelectionRequest("session-1", "device-1", 1, List.of()), offer.getCreatives().get(1));
    }

    @Test
    void selectCandidates_ranksByTotalCreativeValue() {
        SessionOffer expensiveShort = bid(
                UUID.randomUUID(),
                "campaign-a",
                new BigDecimal("10.00"),
                List.of(new CreativeSnapshot("creative-a1", "https://cdn/a1.mp4", 1))
        );
        SessionOffer cheaperLong = bid(
                UUID.randomUUID(),
                "campaign-b",
                new BigDecimal("6.00"),
                List.of(new CreativeSnapshot("creative-b1", "https://cdn/b1.mp4", 3))
        );

        when(sessionOfferRepository.findBySessionIdAndStatus("session-1", OfferStatus.ACTIVE))
                .thenReturn(List.of(expensiveShort, cheaperLong));
        when(selectionScoringService.candidateForOffer(eq(expensiveShort), any(), any(), any(), any()))
                .thenReturn(new SelectionCandidatePick(expensiveShort, expensiveShort.getCreatives().get(0), new BigDecimal("10.00")));
        when(selectionScoringService.candidateForOffer(eq(cheaperLong), any(), any(), any(), any()))
                .thenReturn(new SelectionCandidatePick(cheaperLong, cheaperLong.getCreatives().get(0), new BigDecimal("18.00")));
        when(selectionReservationService.consumeBudgetForSelection(eq(cheaperLong), any(), eq(cheaperLong.getCreatives().get(0)))).thenReturn(OptionalLong.of(8_200L));
        when(receiptTokenService.generateReceiptId(eq("session-1"), any(), any(), any(), any(), any()))
                .thenReturn("receipt-1");

        List<SelectedCandidate> result = selectionCandidateService.selectCandidates(
                new CandidateSelectionRequest("session-1", "device-1", 1, List.of())
        );

        assertEquals(1, result.size());
        assertEquals("creative-b1", result.getFirst().creativeId());
        assertEquals(3, result.getFirst().slotCount());
        verify(selectionReservationService).reserveCreativeForDevice(cheaperLong, new CandidateSelectionRequest("session-1", "device-1", 1, List.of()), cheaperLong.getCreatives().get(0));
    }

    @Test
    void selectCandidates_failsFastWhenSessionLockIsBusy() {
        doThrow(new SessionSelectionBusyException("session-1"))
                .when(selectionLockService).withSessionLock(anyString(), any());

        assertThrows(
                SessionSelectionBusyException.class,
                () -> selectionCandidateService.selectCandidates(
                        new CandidateSelectionRequest("session-1", "device-1", 1, List.of())
                )
        );

        verify(sessionOfferRepository, never()).findBySessionIdAndStatus(anyString(), any());
    }

    @Test
    void selectCandidates_returnsDomainUnavailableWhenRedisFails() {
        doThrow(new SelectionInfrastructureUnavailableException("Redis"))
                .when(selectionLockService).withSessionLock(anyString(), any());

        assertThrows(
                SelectionInfrastructureUnavailableException.class,
                () -> selectionCandidateService.selectCandidates(
                        new CandidateSelectionRequest("session-1", "device-1", 1, List.of())
                )
        );
    }

    @Test
    void selectCandidates_rehydratesBudgetWhenHotStateIsMissing() {
        SessionOffer offer = bid(
                UUID.randomUUID(),
                "campaign-a",
                new BigDecimal("10.00"),
                List.of(new CreativeSnapshot("creative-a1", "https://cdn/a1.mp4", 1))
        );

        when(sessionOfferRepository.findBySessionIdAndStatus("session-1", OfferStatus.ACTIVE))
                .thenReturn(List.of(offer));
        when(selectionScoringService.candidateForOffer(eq(offer), any(), any(), any(), any()))
                .thenReturn(new SelectionCandidatePick(offer, offer.getCreatives().get(0), new BigDecimal("10.00")));
        when(selectionReservationService.consumeBudgetForSelection(eq(offer), any(), eq(offer.getCreatives().get(0)))).thenReturn(OptionalLong.of(0L));
        when(receiptTokenService.generateReceiptId(eq("session-1"), any(), any(), any(), any(), any()))
                .thenReturn("receipt-1");

        List<SelectedCandidate> result = selectionCandidateService.selectCandidates(
                new CandidateSelectionRequest("session-1", "device-1", 1, List.of())
        );

        assertEquals(1, result.size());
        verify(selectionReservationService).consumeBudgetForSelection(eq(offer), any(), eq(offer.getCreatives().get(0)));
    }

    @Test
    void selectCandidates_exhaustsOfferWhenNoCreativeFitsRemainingBudget() {
        SessionOffer offer = bid(
                UUID.randomUUID(),
                "campaign-a",
                new BigDecimal("10.00"),
                List.of(new CreativeSnapshot("creative-a1", "https://cdn/a1.mp4", 3))
        );

        when(sessionOfferRepository.findBySessionIdAndStatus("session-1", OfferStatus.ACTIVE))
                .thenReturn(List.of(offer));
        when(selectionScoringService.candidateForOffer(eq(offer), any(), any(), any(), any()))
                .thenReturn(SelectionCandidatePick.empty(offer));

        List<SelectedCandidate> result = selectionCandidateService.selectCandidates(
                new CandidateSelectionRequest("session-1", "device-1", 1, List.of())
        );

        assertEquals(0, result.size());
        verify(selectionReservationService, never()).reserveCreativeForDevice(any(), any(), any());
    }

    private SessionOffer bid(UUID id, String campaignId, BigDecimal price, List<CreativeSnapshot> creatives) {
        return SessionOffer.builder()
                .id(id)
                .sessionId("session-1")
                .advertiserId("advertiser-1")
                .campaignId(campaignId)
                .totalBudget(new BigDecimal("100.00"))
                .pricePerSlot(price)
                .deviceCooldownSeconds(300)
                .creatives(creatives)
                .status(OfferStatus.ACTIVE)
                .build();
    }
}
