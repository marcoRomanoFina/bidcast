package com.bidcast.wallet_service.wallet;

import com.bidcast.wallet_service.core.exception.WalletNotFoundException;
import com.bidcast.wallet_service.transaction.WalletTransactionRepository;
import com.bidcast.wallet_service.transaction.WalletTransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class WalletServiceIntegrationTest {

    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        postgres.start();
    }

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletTransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
    }

    @Test
    void credit_isIdempotentWhenTheSameReferenceIsProcessedTwice() {
        UUID ownerId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        walletService.credit(ownerId, WalletOwnerType.ADVERTISER, new BigDecimal("250.00"), paymentId, "PAYMENT_TOPUP");
        walletService.credit(ownerId, WalletOwnerType.ADVERTISER, new BigDecimal("250.00"), paymentId, "PAYMENT_TOPUP");

        Wallet wallet = walletRepository.findByOwnerIdAndOwnerType(ownerId, WalletOwnerType.ADVERTISER).orElseThrow();
        assertEquals(0, wallet.getBalance().compareTo(new BigDecimal("250.00")));
        assertEquals(1, walletRepository.count());
        assertEquals(1, transactionRepository.count());
        assertEquals(true, transactionRepository.existsByReferenceIdAndType(paymentId, WalletTransactionType.DEPOSIT));
    }

    @Test
    void getWalletByOwner_throwsDomainExceptionWhenWalletDoesNotExist() {
        assertThrows(WalletNotFoundException.class,
                () -> walletService.getWalletByOwner(UUID.randomUUID(), WalletOwnerType.ADVERTISER));
    }
}
