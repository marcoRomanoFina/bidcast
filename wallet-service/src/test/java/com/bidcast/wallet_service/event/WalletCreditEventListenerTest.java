package com.bidcast.wallet_service.event;

import com.bidcast.wallet_service.event.dto.WalletCreditMessage;
import com.bidcast.wallet_service.wallet.WalletOwnerType;
import com.bidcast.wallet_service.wallet.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WalletCreditEventListenerTest {

    @Mock
    private WalletService walletService;

    @InjectMocks
    private WalletCreditEventListener listener;

    @Test
    void handleWalletCredit_delegatesToWalletService() {
        WalletCreditMessage message = new WalletCreditMessage(
                UUID.randomUUID(),
                new BigDecimal("150.00"),
                UUID.randomUUID(),
                "payment-ref"
        );

        listener.handleWalletCredit(message);

        verify(walletService).credit(
                message.advertiserId(),
                WalletOwnerType.ADVERTISER,
                message.amount(),
                message.paymentId(),
                "PAYMENT_TOPUP"
        );
    }

    @Test
    void handleWalletCredit_swallowsServiceException() {
        WalletCreditMessage message = new WalletCreditMessage(
                UUID.randomUUID(),
                new BigDecimal("150.00"),
                UUID.randomUUID(),
                "payment-ref"
        );

        doThrow(new RuntimeException("boom")).when(walletService).credit(
                message.advertiserId(),
                WalletOwnerType.ADVERTISER,
                message.amount(),
                message.paymentId(),
                "PAYMENT_TOPUP"
        );

        listener.handleWalletCredit(message);

        verify(walletService).credit(
                message.advertiserId(),
                WalletOwnerType.ADVERTISER,
                message.amount(),
                message.paymentId(),
                "PAYMENT_TOPUP"
        );
    }
}
