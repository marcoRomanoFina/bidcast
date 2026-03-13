package com.bidcast.wallet_service.charge;

import com.bidcast.wallet_service.dto.SessionSettlementCommand;
import com.bidcast.wallet_service.transaction.WalletTransaction;
import com.bidcast.wallet_service.transaction.WalletTransactionRepository;
import com.bidcast.wallet_service.transaction.WalletTransactionType;
import com.bidcast.wallet_service.wallet.Wallet;
import com.bidcast.wallet_service.wallet.WalletOwnerType;
import com.bidcast.wallet_service.wallet.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionSettlementService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;

    private static final BigDecimal PLATFORM_FEE_PERCENTAGE = new BigDecimal("0.10");

    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional
    public void processSettlement(SessionSettlementCommand command) {
        log.info("Procesando liquidación de sesión para la puja {}. Gasto Total: {}", command.bidId(), command.totalSpent());

        UUID bidUuid = UUID.fromString(command.bidId());
        
        boolean alreadyProcessed = transactionRepository.existsByReferenceIdAndType(
                bidUuid, 
                WalletTransactionType.POP_CHARGE_ADVERTISER_DEBIT
        );

        if (alreadyProcessed) {
            log.warn("Idempotencia: La liquidación para la puja {} ya fue procesada. Ignorando.", command.bidId());
            return;
        }

        Wallet advertiserWallet = getWalletOrThrow(UUID.fromString(command.advertiserId()), WalletOwnerType.ADVERTISER);
        Wallet publisherWallet = getWalletOrThrow(UUID.fromString(command.publisherId()), WalletOwnerType.PUBLISHER);
        
        Wallet platformWallet = walletRepository.findByOwnerType(WalletOwnerType.PLATFORM)
                .stream().findFirst()
                .orElseThrow(() -> new IllegalStateException("Billetera de la plataforma no encontrada"));

        // 1. Cálculos de Reparto
        BigDecimal spent = command.totalSpent();
        BigDecimal platformFee = spent.multiply(PLATFORM_FEE_PERCENTAGE);
        BigDecimal publisherNet = spent.subtract(platformFee);
        
        // 2. Ejecutar Movimientos
        advertiserWallet.settle(spent);
        
        BigDecimal refundAmount = command.initialBudget().subtract(spent);
        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            advertiserWallet.unfreeze(refundAmount);
            log.info("Devueltos {} al anunciante {} por la puja {}", refundAmount, command.advertiserId(), command.bidId());
        }

        publisherWallet.credit(publisherNet);
        platformWallet.credit(platformFee);

        // 3. Registrar Transacciones
        var advertiserEntry = WalletTransaction.debitForProofOfPlay(advertiserWallet, spent, bidUuid);
        var publisherEntry = WalletTransaction.creditForPublisher(publisherWallet, publisherNet, bidUuid);
        var platformEntry = WalletTransaction.creditForPlatform(platformWallet, platformFee, bidUuid);

        transactionRepository.saveAll(List.of(advertiserEntry, publisherEntry, platformEntry));
        walletRepository.saveAll(List.of(advertiserWallet, publisherWallet, platformWallet));

        log.info("Liquidación exitosa para la puja {}. El Publisher recibió {}, la Plataforma recibió {}", 
                command.bidId(), publisherNet, platformFee);
    }

    private Wallet getWalletOrThrow(UUID ownerId, WalletOwnerType type) {
        return walletRepository.findByOwnerIdAndOwnerType(ownerId, type)
                .orElseThrow(() -> new IllegalArgumentException("Billetera no encontrada para " + type + " " + ownerId));
    }
}
