package com.bidcast.auction_service.bid;

import com.bidcast.auction_service.pop.ProofOfPlayRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BidRehydrationServiceTest {

    @Mock private SessionBidRepository sessionBidRepository;
    @Mock private ProofOfPlayRepository popRepository;
    @Mock private BidInfrastructureService infrastructureService;

    @InjectMocks
    private BidRehydrationService rehydrationService;

    private UUID bidId = UUID.randomUUID();
    private SessionBid bid;

    @BeforeEach
    void setUp() {
        bid = SessionBid.builder()
                .id(bidId)
                .totalBudget(new BigDecimal("10.00"))
                .build();
    }

    @Test
    @DisplayName("Cálculo de Saldo Real: Resta el costo sumado de ProofOfPlay al presupuesto total")
    void calculateRealBalanceCents_SubtractsSpentAmount() {
        when(popRepository.sumCostByBidId(bidId.toString())).thenReturn(new BigDecimal("1.50"));
        long balanceCents = rehydrationService.calculateRealBalanceCents(bid);
        assertEquals(850, balanceCents);
    }

    @Test
    @DisplayName("Rehidratación de Bid: Recupera de DB e inyecta en Redis")
    void rehydrateFullBid_Success() {
        when(sessionBidRepository.findById(bidId)).thenReturn(Optional.of(bid));
        when(popRepository.sumCostByBidId(bidId.toString())).thenReturn(BigDecimal.ZERO);

        RestoredBid restored = rehydrationService.rehydrateFullBid(bidId);

        assertEquals(bidId, restored.metadata().id());
        assertEquals(1000, restored.balanceCents());
    }
}
