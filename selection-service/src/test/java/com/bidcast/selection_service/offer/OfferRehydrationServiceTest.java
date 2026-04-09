package com.bidcast.selection_service.offer;

import com.bidcast.selection_service.pop.ProofOfPlayRepository;
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
class OfferRehydrationServiceTest {

    @Mock private SessionOfferRepository sessionOfferRepository;
    @Mock private ProofOfPlayRepository popRepository;
    @Mock private OfferInfrastructureService infrastructureService;

    @InjectMocks
    private OfferRehydrationService rehydrationService;

    private UUID offerId = UUID.randomUUID();
    private SessionOffer offer;

    @BeforeEach
    void setUp() {
        offer = SessionOffer.builder()
                .id(offerId)
                .sessionId("session-1")
                .advertiserId("adv-1")
                .campaignId("camp-1")
                .totalBudget(new BigDecimal("10.00"))
                .pricePerSlot(new BigDecimal("1.00"))
                .deviceCooldownSeconds(300)
                .creatives(java.util.List.of(
                        new CreativeSnapshot("creative-1", "url-1", 1),
                        new CreativeSnapshot("creative-2", "url-2", 3)
                ))
                .build();
    }

    @Test
    @DisplayName("Calculo de saldo real: resta el costo sumado de ProofOfPlay al presupuesto total")
    void calculateRealBalanceCents_SubtractsSpentAmount() {
        when(popRepository.sumCostByOfferId(offerId.toString())).thenReturn(new BigDecimal("1.50"));
        long balanceCents = rehydrationService.calculateRealBalanceCents(offer);
        assertEquals(850, balanceCents);
    }

    @Test
    @DisplayName("Rehidratacion de offer: recupera de DB e inyecta en Redis")
    void rehydrateOffer_Success() {
        when(sessionOfferRepository.findById(offerId)).thenReturn(Optional.of(offer));
        when(popRepository.sumCostByOfferId(offerId.toString())).thenReturn(BigDecimal.ZERO);

        RestoredOffer restored = rehydrationService.rehydrateOffer(offerId);

        assertEquals(offerId, restored.metadata().id());
        assertEquals(1000, restored.balanceCents());
        assertEquals(2, restored.metadata().creatives().size());
    }
}
