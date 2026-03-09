package com.bidcast.wallet_service.charge;

import com.bidcast.wallet_service.TestcontainersConfiguration;
import com.bidcast.wallet_service.transaction.WalletTransaction;
import com.bidcast.wallet_service.transaction.WalletTransactionRepository;
import com.bidcast.wallet_service.transaction.WalletTransactionType;
import com.bidcast.wallet_service.wallet.Wallet;
import com.bidcast.wallet_service.wallet.WalletOwnerType;
import com.bidcast.wallet_service.wallet.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ProofOfPlaySettlementServiceIntegrationTest {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletTransactionRepository transactionRepository;

    @Autowired
    private ProofOfPlayChargeRepository chargeRepository;

    @Autowired
    private ProofOfPlaySettlementService settlementService;

    private Wallet advertiserWallet;
    private Wallet publisherWallet;
    private Wallet platformWallet;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        chargeRepository.deleteAll();
        walletRepository.deleteAll();

        advertiserWallet = walletRepository.save(
                Wallet.builder()
                        .ownerId(UUID.randomUUID())
                        .ownerType(WalletOwnerType.ADVERTISER)
                        .currencyCode("USD")
                        .balance(new BigDecimal("100.0000"))
                        .build()
        );

        publisherWallet = walletRepository.save(
                Wallet.builder()
                        .ownerId(UUID.randomUUID())
                        .ownerType(WalletOwnerType.PUBLISHER)
                        .currencyCode("USD")
                        .balance(BigDecimal.ZERO)
                        .build()
        );

        platformWallet = walletRepository.save(
                Wallet.builder()
                        .ownerId(UUID.randomUUID())
                        .ownerType(WalletOwnerType.PLATFORM)
                        .currencyCode("USD")
                        .balance(BigDecimal.ZERO)
                        .build()
        );
    }

    @Test
    void processProofOfPlayCharge_updatesWalletsAndCreatesLedgerEntries() {
        UUID proofOfPlayId = UUID.randomUUID();
        BigDecimal gross = new BigDecimal("1.0000");
        BigDecimal publisherAmount = new BigDecimal("0.7000");
        BigDecimal platformFee = new BigDecimal("0.3000");

        ProofOfPlayChargeCommand command = new ProofOfPlayChargeCommand(
                proofOfPlayId,
                gross,
                publisherAmount,
                platformFee,
                advertiserWallet.getId(),
                publisherWallet.getId(),
                platformWallet.getId()
        );

        settlementService.processProofOfPlayCharge(command);

        Wallet updatedAdvertiser = walletRepository.findById(advertiserWallet.getId()).orElseThrow();
        Wallet updatedPublisher = walletRepository.findById(publisherWallet.getId()).orElseThrow();
        Wallet updatedPlatform = walletRepository.findById(platformWallet.getId()).orElseThrow();

        assertThat(updatedAdvertiser.getBalance()).isEqualByComparingTo(new BigDecimal("99.0000"));
        assertThat(updatedPublisher.getBalance()).isEqualByComparingTo(new BigDecimal("0.7000"));
        assertThat(updatedPlatform.getBalance()).isEqualByComparingTo(new BigDecimal("0.3000"));

        List<WalletTransaction> transactions = transactionRepository.findAll();
        assertThat(transactions).hasSize(3);

        assertThat(transactions)
                .extracting(WalletTransaction::getType)
                .containsExactlyInAnyOrder(
                        WalletTransactionType.POP_CHARGE_ADVERTISER_DEBIT,
                        WalletTransactionType.POP_PUBLISHER_CREDIT,
                        WalletTransactionType.POP_PLATFORM_FEE
                );

        assertThat(chargeRepository.count()).isEqualTo(1);
    }

    @Test
    void processProofOfPlayCharge_isIdempotentForSameProofOfPlayId() {
        UUID proofOfPlayId = UUID.randomUUID();
        BigDecimal gross = new BigDecimal("1.0000");
        BigDecimal publisherAmount = new BigDecimal("0.7000");
        BigDecimal platformFee = new BigDecimal("0.3000");

        ProofOfPlayChargeCommand command = new ProofOfPlayChargeCommand(
                proofOfPlayId,
                gross,
                publisherAmount,
                platformFee,
                advertiserWallet.getId(),
                publisherWallet.getId(),
                platformWallet.getId()
        );

        settlementService.processProofOfPlayCharge(command);
        // Segundo llamado con el mismo proofOfPlayId debe ser un no-op
        settlementService.processProofOfPlayCharge(command);

        Wallet updatedAdvertiser = walletRepository.findById(advertiserWallet.getId()).orElseThrow();
        Wallet updatedPublisher = walletRepository.findById(publisherWallet.getId()).orElseThrow();
        Wallet updatedPlatform = walletRepository.findById(platformWallet.getId()).orElseThrow();

        assertThat(updatedAdvertiser.getBalance()).isEqualByComparingTo(new BigDecimal("99.0000"));
        assertThat(updatedPublisher.getBalance()).isEqualByComparingTo(new BigDecimal("0.7000"));
        assertThat(updatedPlatform.getBalance()).isEqualByComparingTo(new BigDecimal("0.3000"));

        List<WalletTransaction> transactions = transactionRepository.findAll();
        assertThat(transactions).hasSize(3);
        assertThat(chargeRepository.count()).isEqualTo(1);
    }
}

