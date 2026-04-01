package com.bidcast.wallet_service.event;

import com.bidcast.wallet_service.wallet.WalletOwnerType;
import com.bidcast.wallet_service.wallet.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WalletCreditEventListenerTest {

    @Mock
    private WalletService walletService;

    @InjectMocks
    private WalletCreditEventListener listener;

    @Test
    void handlePaymentConfirmed_delegatesToWalletService() {
        PaymentConfirmedEvent event = new PaymentConfirmedEvent(
                UUID.randomUUID(),
                Instant.now(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("150.00")
        );

        listener.handlePaymentConfirmed(event);

        verify(walletService).credit(
                event.advertiserId(),
                WalletOwnerType.ADVERTISER,
                event.amount(),
                event.paymentId(),
                "PAYMENT_TOPUP"
        );
    }

    @Test
    void handlePaymentConfirmed_throwsExceptionOnFailure() {
        PaymentConfirmedEvent event = new PaymentConfirmedEvent(
                UUID.randomUUID(),
                Instant.now(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("150.00")
        );

        doThrow(new RuntimeException("boom")).when(walletService).credit(
                event.advertiserId(),
                WalletOwnerType.ADVERTISER,
                event.amount(),
                event.paymentId(),
                "PAYMENT_TOPUP"
        );

        assertThrows(RuntimeException.class, () -> listener.handlePaymentConfirmed(event));

        verify(walletService).credit(
                event.advertiserId(),
                WalletOwnerType.ADVERTISER,
                event.amount(),
                event.paymentId(),
                "PAYMENT_TOPUP"
        );
    }
}
