package com.bidcast.wallet_service.charge;

import com.bidcast.wallet_service.charge.dto.SessionSettlementCommand;
import com.bidcast.wallet_service.transaction.WalletTransactionRepository;
import com.bidcast.wallet_service.wallet.Wallet;
import com.bidcast.wallet_service.wallet.WalletOwnerType;
import com.bidcast.wallet_service.wallet.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class SessionSettlementServiceIntegrationTest {

    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        postgres.start();
    }

    @Autowired
    private SessionSettlementService settlementService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletTransactionRepository transactionRepository;

    private UUID advertiserId = UUID.randomUUID();
    private UUID publisherId = UUID.randomUUID();
    private Wallet advertiserWallet;
    private Wallet publisherWallet;
    private Wallet platformWallet;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        walletRepository.deleteAll();

        advertiserWallet = walletRepository.save(Wallet.builder()
                .ownerId(advertiserId)
                .ownerType(WalletOwnerType.ADVERTISER)
                .balance(new BigDecimal("1000.00"))
                .frozenBalance(new BigDecimal("100.00")) // Presupuesto congelado para una sesión
                .currencyCode("ARS")
                .build());

        publisherWallet = walletRepository.save(Wallet.builder()
                .ownerId(publisherId)
                .ownerType(WalletOwnerType.PUBLISHER)
                .balance(BigDecimal.ZERO)
                .currencyCode("ARS")
                .build());

        platformWallet = walletRepository.save(Wallet.builder()
                .ownerId(UUID.randomUUID())
                .ownerType(WalletOwnerType.PLATFORM)
                .balance(BigDecimal.ZERO)
                .currencyCode("ARS")
                .build());
    }

    @Test
    @DisplayName("La liquidación de sesión distribuye fondos correctamente e idempotencia funciona")
    @Transactional
    void shouldSettleSessionSuccessfully() {
        // GIVEN
        UUID bidId = UUID.randomUUID();
        BigDecimal totalSpent = new BigDecimal("40.00");
        BigDecimal initialBudget = new BigDecimal("100.00");

        SessionSettlementCommand command = new SessionSettlementCommand(
                bidId.toString(),
                UUID.randomUUID().toString(),
                advertiserId.toString(),
                publisherId.toString(),
                totalSpent,
                initialBudget
        );

        // WHEN: Primer intento
        settlementService.processSettlement(command);

        // THEN: Verificamos saldos
        Wallet updatedAdv = walletRepository.findById(advertiserWallet.getId()).orElseThrow();
        Wallet updatedPub = walletRepository.findById(publisherWallet.getId()).orElseThrow();
        Wallet updatedPlat = walletRepository.findById(platformWallet.getId()).orElseThrow();

        // Advertiser: 1000 - (100 congelados) + (100 - 40 devueltos) = 1060
        // O más simple: balance inicial (1000) + refund (60) = 1060
        assertEquals(new BigDecimal("1060.0000"), updatedAdv.getBalance().setScale(4, RoundingMode.HALF_UP));
        assertEquals(BigDecimal.ZERO.setScale(4), updatedAdv.getFrozenBalance().setScale(4));

        // Fees: 40 * 0.10 = 4.00
        // Publisher: 40 - 4 = 36
        assertEquals(new BigDecimal("36.0000"), updatedPub.getBalance().setScale(4, RoundingMode.HALF_UP));
        assertEquals(new BigDecimal("4.0000"), updatedPlat.getBalance().setScale(4, RoundingMode.HALF_UP));

        // Ledger: Debería haber 3 entradas
        long transactionCount = transactionRepository.count();
        assertEquals(3, transactionCount);

        // WHEN: Intento duplicado (Idempotencia)
        settlementService.processSettlement(command);

        // THEN: El contador de transacciones no debería cambiar
        assertEquals(3, transactionRepository.count());
    }
}
