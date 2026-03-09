package com.bidcast.wallet_service.charge;

import com.bidcast.wallet_service.core.exception.InvalidProofOfPlayChargeException;
import com.bidcast.wallet_service.transaction.WalletTransaction;
import com.bidcast.wallet_service.transaction.WalletTransactionRepository;
import com.bidcast.wallet_service.wallet.Wallet;
import com.bidcast.wallet_service.wallet.WalletRepository;
import com.bidcast.wallet_service.wallet.WalletOwnerType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProofOfPlaySettlementService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final ProofOfPlayChargeRepository chargeRepository;

    @Retryable(
            retryFor = ObjectOptimisticLockingFailureException.class, 
            maxAttempts = 3,                                          
            backoff = @Backoff(delay = 100)                           
    )
    @Transactional
    public void processProofOfPlayCharge(ProofOfPlayChargeCommand command) {
        if (!command.isSumConsistent()) {
            throw new InvalidProofOfPlayChargeException(
                    "La suma publisherAmount + platformFeeAmount debe ser igual a grossAmount"
            );
        }

        // Idempotencia rápida: si ya existe un cargo para este proofOfPlayId, no hacemos nada.
        if (chargeRepository.findByProofOfPlayId(command.proofOfPlayId()).isPresent()) {
            log.info("Idempotency hit (pre-check): ProofOfPlay {} ya fue cobrado. Ignorando.", command.proofOfPlayId());
            return;
        }

        ProofOfPlayCharge charge = ProofOfPlayCharge.builder()
                .proofOfPlayId(command.proofOfPlayId()) 
                .grossAmount(command.grossAmount())
                .publisherAmount(command.publisherAmount())
                .platformFeeAmount(command.platformFeeAmount())
                .advertiserWalletId(command.advertiserWalletId())
                .publisherWalletId(command.publisherWalletId())
                .platformWalletId(command.platformWalletId())
                .build();

        chargeRepository.save(charge);

        Wallet advertiserWallet = getWalletOrThrow(command.advertiserWalletId());
        Wallet publisherWallet = getWalletOrThrow(command.publisherWalletId());
        Wallet platformWallet = getWalletOrThrow(command.platformWalletId());

        validateWalletRoles(advertiserWallet, publisherWallet, platformWallet);

        advertiserWallet.debit(command.grossAmount());
        publisherWallet.credit(command.publisherAmount());
        platformWallet.credit(command.platformFeeAmount());

        var advertiserEntry = WalletTransaction.debitForProofOfPlay(
                advertiserWallet,
                command.grossAmount(),
                command.proofOfPlayId()
        );

        var publisherEntry = WalletTransaction.creditForPublisher(
                publisherWallet,
                command.publisherAmount(),
                command.proofOfPlayId()
        );

        var platformEntry = WalletTransaction.creditForPlatform(
                platformWallet,
                command.platformFeeAmount(),
                command.proofOfPlayId()
        );

        transactionRepository.saveAll(List.of(
                advertiserEntry,
                publisherEntry,
                platformEntry
        ));

        walletRepository.saveAll(List.of(
                advertiserWallet,
                publisherWallet,
                platformWallet
        ));

        log.info(
                "ProofOfPlay {} liquidado con éxito. grossAmount={}, publisherAmount={}, platformFeeAmount={}",
                command.proofOfPlayId(),
                command.grossAmount(),
                command.publisherAmount(),
                command.platformFeeAmount()
        );
    }

    private Wallet getWalletOrThrow(UUID walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found: " + walletId));
    }

    private void validateWalletRoles(Wallet advertiser, Wallet publisher, Wallet platform) {
        validateWalletRole(advertiser, WalletOwnerType.ADVERTISER, "advertiser");
        validateWalletRole(publisher,  WalletOwnerType.PUBLISHER,  "publisher");
        validateWalletRole(platform,   WalletOwnerType.PLATFORM,   "platform");
    }
    
    private void validateWalletRole(Wallet wallet, WalletOwnerType expected, String roleName) {
        if (wallet.getOwnerType() != expected) {
            throw new IllegalArgumentException(
                    "Wallet de " + roleName + " no tiene tipo " + expected
            );
        }
    }

}