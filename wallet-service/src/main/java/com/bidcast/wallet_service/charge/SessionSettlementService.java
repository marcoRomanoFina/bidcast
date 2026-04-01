package com.bidcast.wallet_service.charge;

import com.bidcast.wallet_service.core.exception.PlatformWalletNotConfiguredException;
import com.bidcast.wallet_service.core.exception.WalletNotFoundException;
import com.bidcast.wallet_service.event.SessionSettledEvent;
import com.bidcast.wallet_service.transaction.WalletTransaction;
import com.bidcast.wallet_service.transaction.WalletTransactionRepository;
import com.bidcast.wallet_service.transaction.WalletTransactionType;
import com.bidcast.wallet_service.wallet.Wallet;
import com.bidcast.wallet_service.wallet.WalletOwnerType;
import com.bidcast.wallet_service.wallet.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

/**
 * Servicio de liquidación final de sesiones de puja (Settlement Engine).
 * Se encarga de reconciliar el presupuesto congelado contra el gasto real,
 * repartiendo los fondos entre Publisher y Plataforma, y devolviendo el sobrante al Anunciante.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionSettlementService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;

    // Política de comisión (Externalizable en el futuro)
    private static final BigDecimal PLATFORM_FEE_RATE = new BigDecimal("0.10");

    @Retryable(
        retryFor = ObjectOptimisticLockingFailureException.class, 
        maxAttempts = 5, 
        backoff = @Backoff(delay = 100, multiplier = 2)
    )
    @Transactional
    public void processSettlement(SessionSettledEvent event) {
        log.info("Starting financial settlement for bid {}. Spent: {}/{}", 
                event.bidId(), event.totalSpent(), event.initialBudget());

        UUID bidId = UUID.fromString(event.bidId());
        
        // 1. BLINDAJE DE IDEMPOTENCIA
        if (isAlreadySettled(bidId)) {
            log.warn("Idempotency hit: settlement for {} was already processed.", bidId);
            return;
        }

        // 2. RECUPERACIÓN DE ESTADO
        Wallet advertiser = getWalletOrThrow(UUID.fromString(event.advertiserId()), WalletOwnerType.ADVERTISER);
        Wallet publisher = getWalletOrThrow(UUID.fromString(event.publisherId()), WalletOwnerType.PUBLISHER);
        Wallet platform = getPlatformWallet();

        // 3. CÁLCULO DE REPARTO (Split logic)
        BigDecimal spent = event.totalSpent();
        BigDecimal platformFee = spent.multiply(PLATFORM_FEE_RATE).setScale(4, RoundingMode.HALF_UP);
        BigDecimal publisherNet = spent.subtract(platformFee);
        BigDecimal refund = event.initialBudget().subtract(spent);

        // 4. MOVIMIENTOS ATÓMICOS DE DOMINIO
        advertiser.settleAndRefund(spent, event.initialBudget());
        publisher.credit(publisherNet);
        platform.credit(platformFee);

        // 5. REGISTRO EN LEDGER (Double-Entry principles)
        recordLedgerEntries(bidId, advertiser, publisher, platform, spent, publisherNet, platformFee);

        // 6. PERSISTENCIA
        walletRepository.saveAll(List.of(advertiser, publisher, platform));

        log.info("Settlement completed for bid {}. Refund: {}, Publisher: {}, Fee: {}", 
                bidId, refund, publisherNet, platformFee);
    }

    private boolean isAlreadySettled(UUID bidId) {
        return transactionRepository.existsByReferenceIdAndType(
                bidId, 
                WalletTransactionType.POP_CHARGE_ADVERTISER_DEBIT
        );
    }

    private void recordLedgerEntries(UUID bidId, Wallet adv, Wallet pub, Wallet plat, 
                                   BigDecimal spent, BigDecimal pubNet, BigDecimal fee) {
        
        var entries = List.of(
            WalletTransaction.debitForProofOfPlay(adv, spent, bidId),
            WalletTransaction.creditForPublisher(pub, pubNet, bidId),
            WalletTransaction.creditForPlatform(plat, fee, bidId)
        );
        
        try {
            transactionRepository.saveAll(entries);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Persistent idempotency hit: settlement for {} is already recorded.", bidId);
        }
    }

    private Wallet getWalletOrThrow(UUID ownerId, WalletOwnerType type) {
        return walletRepository.findByOwnerIdAndOwnerType(ownerId, type)
                .orElseThrow(() -> new WalletNotFoundException(ownerId));
    }

    private Wallet getPlatformWallet() {
        return walletRepository.findByOwnerType(WalletOwnerType.PLATFORM)
                .stream()
                .findFirst()
                .orElseThrow(PlatformWalletNotConfiguredException::new);
    }
}
