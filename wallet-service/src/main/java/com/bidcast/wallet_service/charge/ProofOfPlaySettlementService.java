package com.bidcast.wallet_service.charge;

import com.bidcast.wallet_service.transaction.WalletTransaction;
import com.bidcast.wallet_service.transaction.WalletTransactionRepository;
import com.bidcast.wallet_service.wallet.Wallet;
import com.bidcast.wallet_service.wallet.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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
            retryFor = ObjectOptimisticLockingFailureException.class, // Solo reintenta si el error es de concurrencia
            maxAttempts = 3,                                          // Máximo 3 intentos para no hacer un bucle infinito
            backoff = @Backoff(delay = 100)                           // Espera 100 milisegundos entre intento e intento
    )
    @Transactional
    public void processProofOfPlayCharge(ProofOfPlayChargeCommand command) {
        
        ProofOfPlayCharge charge = ProofOfPlayCharge.builder()
                .proofOfPlayId(command.proofOfPlayId()) 
                .grossAmount(command.grossAmount())
                .publisherAmount(command.publisherAmount())
                .platformFeeAmount(command.platformFeeAmount())
                .advertiserWalletId(command.advertiserWalletId())
                .publisherWalletId(command.publisherWalletId())
                .platformWalletId(command.platformWalletId())
                .build();

        try {
            chargeRepository.saveAndFlush(charge);
        } catch (DataIntegrityViolationException ex) {
            if (isDuplicateProofOfPlayId(ex)) {
    
                log.info("Idempotency hit: ProofOfPlay {} ya fue cobrado. Ignorando.", command.proofOfPlayId());
                return;
            }
            throw ex;
        }

        Wallet advertiserWallet = getWalletOrThrow(command.advertiserWalletId());
        Wallet publisherWallet = getWalletOrThrow(command.publisherWalletId());
        Wallet platformWallet = getWalletOrThrow(command.platformWalletId());

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
    }

    private Wallet getWalletOrThrow(UUID walletId) {
        return walletRepository.findById(walletId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found: " + walletId));
    }

    private boolean isDuplicateProofOfPlayId(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause() != null
                ? ex.getMostSpecificCause().getMessage().toLowerCase()
                : ex.getMessage().toLowerCase();
        
        return message.contains("duplicate key") || message.contains("proof_of_play_charges_pkey");
    }
}