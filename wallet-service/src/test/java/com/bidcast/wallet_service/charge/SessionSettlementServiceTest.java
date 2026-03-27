package com.bidcast.wallet_service.charge;

import com.bidcast.wallet_service.charge.dto.SessionSettlementCommand;
import com.bidcast.wallet_service.core.exception.PlatformWalletNotConfiguredException;
import com.bidcast.wallet_service.core.exception.WalletNotFoundException;
import com.bidcast.wallet_service.transaction.WalletTransactionRepository;
import com.bidcast.wallet_service.transaction.WalletTransactionType;
import com.bidcast.wallet_service.wallet.Wallet;
import com.bidcast.wallet_service.wallet.WalletOwnerType;
import com.bidcast.wallet_service.wallet.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionSettlementServiceTest {

    private WalletRepository walletRepository;
    private WalletTransactionRepository transactionRepository;
    private SessionSettlementService settlementService;

    @BeforeEach
    void setUp() {
        walletRepository = mock(WalletRepository.class);
        transactionRepository = mock(WalletTransactionRepository.class);
        settlementService = new SessionSettlementService(walletRepository, transactionRepository);
    }

    @Test
    void processSettlement_returnsImmediatelyWhenBidWasAlreadySettled() {
        UUID bidId = UUID.randomUUID();
        SessionSettlementCommand command = commandFor(bidId);

        when(transactionRepository.existsByReferenceIdAndType(bidId, WalletTransactionType.POP_CHARGE_ADVERTISER_DEBIT))
                .thenReturn(true);

        settlementService.processSettlement(command);

        verify(walletRepository, never()).findByOwnerIdAndOwnerType(any(), any());
        verify(transactionRepository, never()).saveAll(any());
    }

    @Test
    void processSettlement_throwsWhenAdvertiserWalletIsMissing() {
        UUID bidId = UUID.randomUUID();
        UUID advertiserId = UUID.randomUUID();
        SessionSettlementCommand command = commandFor(bidId, advertiserId, UUID.randomUUID());

        when(transactionRepository.existsByReferenceIdAndType(bidId, WalletTransactionType.POP_CHARGE_ADVERTISER_DEBIT))
                .thenReturn(false);
        when(walletRepository.findByOwnerIdAndOwnerType(advertiserId, WalletOwnerType.ADVERTISER))
                .thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class, () -> settlementService.processSettlement(command));
    }

    @Test
    void processSettlement_throwsWhenPlatformWalletIsMissing() {
        UUID bidId = UUID.randomUUID();
        UUID advertiserId = UUID.randomUUID();
        UUID publisherId = UUID.randomUUID();
        SessionSettlementCommand command = commandFor(bidId, advertiserId, publisherId);

        when(transactionRepository.existsByReferenceIdAndType(bidId, WalletTransactionType.POP_CHARGE_ADVERTISER_DEBIT))
                .thenReturn(false);
        when(walletRepository.findByOwnerIdAndOwnerType(advertiserId, WalletOwnerType.ADVERTISER))
                .thenReturn(Optional.of(wallet(advertiserId, WalletOwnerType.ADVERTISER, "100.00", "20.00")));
        when(walletRepository.findByOwnerIdAndOwnerType(publisherId, WalletOwnerType.PUBLISHER))
                .thenReturn(Optional.of(wallet(publisherId, WalletOwnerType.PUBLISHER, "0.00", "0.00")));
        when(walletRepository.findByOwnerType(WalletOwnerType.PLATFORM))
                .thenReturn(List.of());

        assertThrows(PlatformWalletNotConfiguredException.class, () -> settlementService.processSettlement(command));
        verify(transactionRepository, never()).saveAll(any());
    }

    @Test
    void processSettlement_treatsLedgerUniqueConstraintRaceAsIdempotent() {
        UUID bidId = UUID.randomUUID();
        UUID advertiserId = UUID.randomUUID();
        UUID publisherId = UUID.randomUUID();
        SessionSettlementCommand command = commandFor(bidId, advertiserId, publisherId);

        when(transactionRepository.existsByReferenceIdAndType(bidId, WalletTransactionType.POP_CHARGE_ADVERTISER_DEBIT))
                .thenReturn(false);
        when(walletRepository.findByOwnerIdAndOwnerType(advertiserId, WalletOwnerType.ADVERTISER))
                .thenReturn(Optional.of(wallet(advertiserId, WalletOwnerType.ADVERTISER, "1000.00", "100.00")));
        when(walletRepository.findByOwnerIdAndOwnerType(publisherId, WalletOwnerType.PUBLISHER))
                .thenReturn(Optional.of(wallet(publisherId, WalletOwnerType.PUBLISHER, "0.00", "0.00")));
        when(walletRepository.findByOwnerType(WalletOwnerType.PLATFORM))
                .thenReturn(List.of(wallet(UUID.randomUUID(), WalletOwnerType.PLATFORM, "0.00", "0.00")));
        doThrow(new DataIntegrityViolationException("duplicate")).when(transactionRepository).saveAll(any());

        assertDoesNotThrow(() -> settlementService.processSettlement(command));
    }

    private SessionSettlementCommand commandFor(UUID bidId) {
        return commandFor(bidId, UUID.randomUUID(), UUID.randomUUID());
    }

    private SessionSettlementCommand commandFor(UUID bidId, UUID advertiserId, UUID publisherId) {
        return new SessionSettlementCommand(
                bidId.toString(),
                UUID.randomUUID().toString(),
                advertiserId.toString(),
                publisherId.toString(),
                new BigDecimal("10.00"),
                new BigDecimal("20.00")
        );
    }

    private Wallet wallet(UUID ownerId, WalletOwnerType ownerType, String balance, String frozenBalance) {
        return Wallet.builder()
                .id(UUID.randomUUID())
                .ownerId(ownerId)
                .ownerType(ownerType)
                .currencyCode("ARS")
                .balance(new BigDecimal(balance))
                .frozenBalance(new BigDecimal(frozenBalance))
                .build();
    }
}
