package com.bidcast.wallet_service.wallet;

import com.bidcast.wallet_service.core.exception.WalletNotFoundException;
import com.bidcast.wallet_service.transaction.WalletTransaction;
import com.bidcast.wallet_service.transaction.WalletTransactionRepository;
import com.bidcast.wallet_service.transaction.WalletTransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WalletServiceTest {

    private WalletRepository walletRepository;
    private WalletTransactionRepository transactionRepository;
    private WalletService walletService;

    @BeforeEach
    void setUp() {
        walletRepository = mock(WalletRepository.class);
        transactionRepository = mock(WalletTransactionRepository.class);
        walletService = new WalletService(walletRepository, transactionRepository);
    }

    @Test
    void credit_createsWalletAndLedgerWhenOwnerDoesNotExist() {
        UUID ownerId = UUID.randomUUID();
        UUID referenceId = UUID.randomUUID();

        when(transactionRepository.existsByReferenceIdAndType(referenceId, WalletTransactionType.DEPOSIT)).thenReturn(false);
        when(walletRepository.findByOwnerIdAndOwnerType(ownerId, WalletOwnerType.ADVERTISER)).thenReturn(Optional.empty());
        when(walletRepository.save(any(Wallet.class))).thenAnswer(invocation -> {
            Wallet wallet = invocation.getArgument(0);
            wallet.setId(UUID.randomUUID());
            return wallet;
        });

        walletService.credit(ownerId, WalletOwnerType.ADVERTISER, new BigDecimal("250.00"), referenceId, "PAYMENT_TOPUP");

        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        verify(walletRepository).save(walletCaptor.capture());
        Wallet persistedWallet = walletCaptor.getValue();
        assertEquals(ownerId, persistedWallet.getOwnerId());
        assertEquals("ARS", persistedWallet.getCurrencyCode());
        assertEquals(0, persistedWallet.getBalance().compareTo(new BigDecimal("250.00")));

        ArgumentCaptor<WalletTransaction> txCaptor = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(transactionRepository).save(txCaptor.capture());
        WalletTransaction ledgerEntry = txCaptor.getValue();
        assertEquals(WalletTransactionType.DEPOSIT, ledgerEntry.getType());
        assertEquals(referenceId, ledgerEntry.getReferenceId());
        assertEquals("PAYMENT_TOPUP", ledgerEntry.getReferenceType());
    }

    @Test
    void credit_returnsImmediatelyWhenReferenceWasAlreadyProcessed() {
        UUID referenceId = UUID.randomUUID();
        when(transactionRepository.existsByReferenceIdAndType(referenceId, WalletTransactionType.DEPOSIT)).thenReturn(true);

        walletService.credit(UUID.randomUUID(), WalletOwnerType.ADVERTISER, new BigDecimal("10.00"), referenceId, "PAYMENT_TOPUP");

        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void credit_treatsUniqueConstraintRaceAsIdempotent() {
        UUID ownerId = UUID.randomUUID();
        UUID referenceId = UUID.randomUUID();
        Wallet wallet = Wallet.builder()
                .id(UUID.randomUUID())
                .ownerId(ownerId)
                .ownerType(WalletOwnerType.ADVERTISER)
                .currencyCode("ARS")
                .balance(BigDecimal.ZERO)
                .frozenBalance(BigDecimal.ZERO)
                .build();

        when(transactionRepository.existsByReferenceIdAndType(referenceId, WalletTransactionType.DEPOSIT)).thenReturn(false);
        when(walletRepository.findByOwnerIdAndOwnerType(ownerId, WalletOwnerType.ADVERTISER)).thenReturn(Optional.of(wallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(wallet);
        doThrow(new DataIntegrityViolationException("duplicate")).when(transactionRepository).save(any());

        walletService.credit(ownerId, WalletOwnerType.ADVERTISER, new BigDecimal("10.00"), referenceId, "PAYMENT_TOPUP");

        verify(walletRepository).save(any(Wallet.class));
        verify(transactionRepository).save(any(WalletTransaction.class));
    }

    @Test
    void getWalletByOwner_returnsWalletWhenFound() {
        UUID ownerId = UUID.randomUUID();
        Wallet wallet = Wallet.builder()
                .id(UUID.randomUUID())
                .ownerId(ownerId)
                .ownerType(WalletOwnerType.ADVERTISER)
                .currencyCode("ARS")
                .build();

        when(walletRepository.findByOwnerIdAndOwnerType(ownerId, WalletOwnerType.ADVERTISER)).thenReturn(Optional.of(wallet));

        Wallet result = walletService.getWalletByOwner(ownerId, WalletOwnerType.ADVERTISER);

        assertEquals(wallet.getId(), result.getId());
    }

    @Test
    void getWalletByOwner_throwsWhenMissing() {
        UUID ownerId = UUID.randomUUID();
        when(walletRepository.findByOwnerIdAndOwnerType(ownerId, WalletOwnerType.ADVERTISER)).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, () -> walletService.getWalletByOwner(ownerId, WalletOwnerType.ADVERTISER));
    }

    @Test
    void freeze_updatesWalletWhenFound() {
        UUID ownerId = UUID.randomUUID();
        Wallet wallet = Wallet.builder()
                .id(UUID.randomUUID())
                .ownerId(ownerId)
                .ownerType(WalletOwnerType.ADVERTISER)
                .currencyCode("ARS")
                .balance(new BigDecimal("100.00"))
                .frozenBalance(BigDecimal.ZERO)
                .build();

        when(walletRepository.findByOwnerIdAndOwnerType(ownerId, WalletOwnerType.ADVERTISER)).thenReturn(Optional.of(wallet));

        walletService.freeze(ownerId, WalletOwnerType.ADVERTISER, new BigDecimal("40.00"), "ref-1");

        assertEquals(0, wallet.getBalance().compareTo(new BigDecimal("60.00")));
        assertEquals(0, wallet.getFrozenBalance().compareTo(new BigDecimal("40.00")));
        verify(walletRepository).save(wallet);
    }

    @Test
    void freeze_throwsWhenWalletDoesNotExist() {
        UUID ownerId = UUID.randomUUID();
        when(walletRepository.findByOwnerIdAndOwnerType(ownerId, WalletOwnerType.ADVERTISER)).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class,
                () -> walletService.freeze(ownerId, WalletOwnerType.ADVERTISER, new BigDecimal("40.00"), "ref-1"));
    }

    @Test
    void unfreeze_updatesWalletWhenFound() {
        UUID ownerId = UUID.randomUUID();
        Wallet wallet = Wallet.builder()
                .id(UUID.randomUUID())
                .ownerId(ownerId)
                .ownerType(WalletOwnerType.ADVERTISER)
                .currencyCode("ARS")
                .balance(new BigDecimal("60.00"))
                .frozenBalance(new BigDecimal("40.00"))
                .build();

        when(walletRepository.findByOwnerIdAndOwnerType(ownerId, WalletOwnerType.ADVERTISER)).thenReturn(Optional.of(wallet));

        walletService.unfreeze(ownerId, WalletOwnerType.ADVERTISER, new BigDecimal("15.00"));

        assertEquals(0, wallet.getBalance().compareTo(new BigDecimal("75.00")));
        assertEquals(0, wallet.getFrozenBalance().compareTo(new BigDecimal("25.00")));
        verify(walletRepository).save(wallet);
    }

    @Test
    void settle_updatesWalletWhenFound() {
        UUID ownerId = UUID.randomUUID();
        Wallet wallet = Wallet.builder()
                .id(UUID.randomUUID())
                .ownerId(ownerId)
                .ownerType(WalletOwnerType.ADVERTISER)
                .currencyCode("ARS")
                .balance(new BigDecimal("60.00"))
                .frozenBalance(new BigDecimal("40.00"))
                .build();

        when(walletRepository.findByOwnerIdAndOwnerType(ownerId, WalletOwnerType.ADVERTISER)).thenReturn(Optional.of(wallet));

        walletService.settle(ownerId, WalletOwnerType.ADVERTISER, new BigDecimal("15.00"));

        assertEquals(0, wallet.getBalance().compareTo(new BigDecimal("60.00")));
        assertEquals(0, wallet.getFrozenBalance().compareTo(new BigDecimal("25.00")));
        verify(walletRepository).save(wallet);
    }
}
