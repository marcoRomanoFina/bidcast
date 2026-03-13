package com.bidcast.wallet_service.wallet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final WalletRepository walletRepository;

    @Transactional
    public void freeze(UUID ownerId, WalletOwnerType ownerType, BigDecimal amount, String referenceId) {
        log.info("Freezing {} for owner {} ({})", amount, ownerId, ownerType);
        
        Wallet wallet = walletRepository.findByOwnerIdAndOwnerType(ownerId, ownerType)
                .orElseThrow(() -> new RuntimeException("Wallet not found for owner " + ownerId));

        wallet.freeze(amount);
        walletRepository.save(wallet);
        
        log.info("Funds frozen successfully for reference {}", referenceId);
    }

    @Transactional
    public void settle(UUID ownerId, WalletOwnerType ownerType, BigDecimal amount) {
        log.info("Settling {} for owner {} ({})", amount, ownerId, ownerType);
        
        Wallet wallet = walletRepository.findByOwnerIdAndOwnerType(ownerId, ownerType)
                .orElseThrow(() -> new RuntimeException("Wallet not found for owner " + ownerId));

        wallet.settle(amount);
        walletRepository.save(wallet);
    }

    @Transactional
    public void unfreeze(UUID ownerId, WalletOwnerType ownerType, BigDecimal amount) {
        log.info("Unfreezing {} for owner {} ({})", amount, ownerId, ownerType);
        
        Wallet wallet = walletRepository.findByOwnerIdAndOwnerType(ownerId, ownerType)
                .orElseThrow(() -> new RuntimeException("Wallet not found for owner " + ownerId));

        wallet.unfreeze(amount);
        walletRepository.save(wallet);
    }
}
