package com.bidcast.wallet_service.wallet;

import com.bidcast.wallet_service.core.exception.WalletNotFoundException;
import com.bidcast.wallet_service.transaction.WalletTransaction;
import com.bidcast.wallet_service.transaction.WalletTransactionRepository;
import com.bidcast.wallet_service.transaction.WalletTransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;

    /**
     * Realiza un crédito (Top-up o Reintegro) en la billetera de un usuario.
     * Incluye blindaje de idempotencia basado en referenceId para evitar duplicados.
     */
    @Transactional
    public void credit(UUID ownerId, WalletOwnerType ownerType, BigDecimal amount, UUID referenceId, String referenceType) {
        log.info("Starting credit of {} for owner {} ({}) - Ref: {}", amount, ownerId, ownerType, referenceId);
        
        // 1. Blindaje de Idempotencia (No acreditar dos veces la misma transacción de pago)
        if (transactionRepository.existsByReferenceIdAndType(referenceId, WalletTransactionType.DEPOSIT)) {
            log.warn("Idempotency hit: credit with reference {} was already processed. Ignoring.", referenceId);
            return;
        }

        // 2. Recuperar o Crear Billetera
        Wallet wallet = walletRepository.findByOwnerIdAndOwnerType(ownerId, ownerType)
                .orElseGet(() -> {
                    log.info("Creating new wallet for owner {} ({})", ownerId, ownerType);
                    return Wallet.builder()
                            .ownerId(ownerId)
                            .ownerType(ownerType)
                            .currencyCode("ARS")
                            .balance(BigDecimal.ZERO)
                            .frozenBalance(BigDecimal.ZERO)
                            .build();
                });

        // 3. Aplicar Crédito en Dominio
        wallet.credit(amount);

        // 4. Persistir la billetera antes del ledger si es nueva, para evitar referenciar una entidad transiente
        Wallet persistedWallet = walletRepository.save(wallet);

        // 5. Registrar en Ledger (Auditoría Histórica)
        var ledgerEntry = WalletTransaction.builder()
                .wallet(persistedWallet)
                .amount(amount)
                .balanceAfter(persistedWallet.getBalance())
                .type(WalletTransactionType.DEPOSIT)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .build();

        // 6. Persistencia Atómica
        try {
            transactionRepository.save(ledgerEntry);
        } catch (DataIntegrityViolationException ex) {
            // Defensa persistente ante carreras: si otro hilo grabó la misma referencia, no duplicamos crédito.
            log.warn("Persistent idempotency hit: credit with reference {} is already recorded.", referenceId);
        }
        
        log.info("Credit completed successfully. New balance: {}", persistedWallet.getBalance());
    }

    @Transactional
    public void freeze(UUID ownerId, WalletOwnerType ownerType, BigDecimal amount, String referenceId) {
        log.info("Freezing {} for owner {} ({})", amount, ownerId, ownerType);
        
        Wallet wallet = walletRepository.findByOwnerIdAndOwnerType(ownerId, ownerType)
                .orElseThrow(() -> new WalletNotFoundException(ownerId));

        wallet.freeze(amount);
        walletRepository.save(wallet);
        
        log.info("Funds frozen successfully for reference {}", referenceId);
    }

    @Transactional
    public void settle(UUID ownerId, WalletOwnerType ownerType, BigDecimal amount) {
        log.info("Settling {} for owner {} ({})", amount, ownerId, ownerType);
        
        Wallet wallet = walletRepository.findByOwnerIdAndOwnerType(ownerId, ownerType)
                .orElseThrow(() -> new WalletNotFoundException(ownerId));

        wallet.settle(amount);
        walletRepository.save(wallet);
    }

    @Transactional
    public void unfreeze(UUID ownerId, WalletOwnerType ownerType, BigDecimal amount) {
        log.info("Unfreezing {} for owner {} ({})", amount, ownerId, ownerType);
        
        Wallet wallet = walletRepository.findByOwnerIdAndOwnerType(ownerId, ownerType)
                .orElseThrow(() -> new WalletNotFoundException(ownerId));

        wallet.unfreeze(amount);
        walletRepository.save(wallet);
    }

    @Transactional(readOnly = true)
    public Wallet getWalletByOwner(UUID ownerId, WalletOwnerType ownerType) {
        return walletRepository.findByOwnerIdAndOwnerType(ownerId, ownerType)
                .orElseThrow(() -> new WalletNotFoundException(ownerId));
    }
}
