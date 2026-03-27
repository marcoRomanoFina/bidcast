package com.bidcast.wallet_service.wallet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
class WalletConcurrencyIntegrationTest {

    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        postgres.start();
    }

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
        walletRepository.deleteAll();
    }

    @Test
    void shouldRejectStaleWalletUpdateWithOptimisticLocking() {
        Wallet persistedWallet = walletRepository.saveAndFlush(Wallet.builder()
                .ownerId(UUID.randomUUID())
                .ownerType(WalletOwnerType.ADVERTISER)
                .currencyCode("ARS")
                .balance(new BigDecimal("100.00"))
                .frozenBalance(BigDecimal.ZERO)
                .build());

        Wallet staleSnapshot = transactionTemplate.execute(status ->
                walletRepository.findById(persistedWallet.getId()).orElseThrow());

        transactionTemplate.executeWithoutResult(status -> {
            Wallet freshSnapshot = walletRepository.findById(persistedWallet.getId()).orElseThrow();
            freshSnapshot.credit(new BigDecimal("15.00"));
            walletRepository.saveAndFlush(freshSnapshot);
        });

        staleSnapshot.credit(new BigDecimal("5.00"));

        assertThrows(ObjectOptimisticLockingFailureException.class, () ->
                transactionTemplate.executeWithoutResult(status -> walletRepository.saveAndFlush(staleSnapshot)));

        Wallet currentWallet = walletRepository.findById(persistedWallet.getId()).orElseThrow();
        assertEquals(0, currentWallet.getBalance().compareTo(new BigDecimal("115.00")));
    }
}
