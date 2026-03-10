package com.bidcast.wallet_service.charge;

import com.bidcast.wallet_service.TestcontainersConfiguration;
import com.bidcast.wallet_service.transaction.WalletTransactionRepository;
import com.bidcast.wallet_service.wallet.Wallet;
import com.bidcast.wallet_service.wallet.WalletOwnerType;
import com.bidcast.wallet_service.wallet.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration.class)
class ProofOfPlayChargeControllerTest {

    @Autowired
    private RestTestClient restClient;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletTransactionRepository transactionRepository;

    @Autowired
    private ProofOfPlayChargeRepository chargeRepository;

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
    void should_return201Created_and_updateWallets_when_requestIsValid() {
        UUID proofOfPlayId = UUID.randomUUID();

        ProofOfPlayChargeCommand command = new ProofOfPlayChargeCommand(
                proofOfPlayId,
                new BigDecimal("1.0000"),
                new BigDecimal("0.7000"),
                new BigDecimal("0.3000"),
                advertiserWallet.getId(),
                publisherWallet.getId(),
                platformWallet.getId()
        );

        var response = restClient.post()
                .uri("/api/v1/proof-of-play-charges")
                .body(command)
                .exchange();

        response.expectStatus().isCreated();

        Wallet updatedAdvertiser = walletRepository.findById(advertiserWallet.getId()).orElseThrow();
        Wallet updatedPublisher = walletRepository.findById(publisherWallet.getId()).orElseThrow();
        Wallet updatedPlatform = walletRepository.findById(platformWallet.getId()).orElseThrow();

        assertThat(updatedAdvertiser.getBalance()).isEqualByComparingTo(new BigDecimal("99.0000"));
        assertThat(updatedPublisher.getBalance()).isEqualByComparingTo(new BigDecimal("0.7000"));
        assertThat(updatedPlatform.getBalance()).isEqualByComparingTo(new BigDecimal("0.3000"));
    }

    @Test
    void should_return400BadRequest_when_requestHasInvalidData() {
        UUID proofOfPlayId = UUID.randomUUID();

        ProofOfPlayChargeCommand invalidCommand = new ProofOfPlayChargeCommand(
                proofOfPlayId,
                new BigDecimal("-1.0000"), 
                new BigDecimal("0.7000"),
                new BigDecimal("0.3000"),
                advertiserWallet.getId(),
                publisherWallet.getId(),
                platformWallet.getId()
        );

        var response = restClient.post()
                .uri("/api/v1/proof-of-play-charges")
                .body(invalidCommand)
                .exchange();

        response.expectStatus().isBadRequest();

        Wallet updatedAdvertiser = walletRepository.findById(advertiserWallet.getId()).orElseThrow();
        Wallet updatedPublisher = walletRepository.findById(publisherWallet.getId()).orElseThrow();
        Wallet updatedPlatform = walletRepository.findById(platformWallet.getId()).orElseThrow();

        assertThat(updatedAdvertiser.getBalance()).isEqualByComparingTo(new BigDecimal("100.0000"));
        assertThat(updatedPublisher.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(updatedPlatform.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(chargeRepository.count()).isZero();
        assertThat(transactionRepository.count()).isZero();
    }

    @Test
    void should_return409Conflict_when_insufficientBalance() {
        // Dejamos al advertiser con saldo 0 para forzar error de saldo insuficiente
        advertiserWallet.setBalance(BigDecimal.ZERO);
        walletRepository.save(advertiserWallet);

        UUID proofOfPlayId = UUID.randomUUID();

        ProofOfPlayChargeCommand command = new ProofOfPlayChargeCommand(
                proofOfPlayId,
                new BigDecimal("1.0000"),
                new BigDecimal("0.7000"),
                new BigDecimal("0.3000"),
                advertiserWallet.getId(),
                publisherWallet.getId(),
                platformWallet.getId()
        );

        var response = restClient.post()
                .uri("/api/v1/proof-of-play-charges")
                .body(command)
                .exchange();

        response.expectStatus().isEqualTo(409);

        Wallet updatedAdvertiser = walletRepository.findById(advertiserWallet.getId()).orElseThrow();
        Wallet updatedPublisher = walletRepository.findById(publisherWallet.getId()).orElseThrow();
        Wallet updatedPlatform = walletRepository.findById(platformWallet.getId()).orElseThrow();

        assertThat(updatedAdvertiser.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(updatedPublisher.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(updatedPlatform.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(chargeRepository.count()).isZero();
        assertThat(transactionRepository.count()).isZero();
    }
}


