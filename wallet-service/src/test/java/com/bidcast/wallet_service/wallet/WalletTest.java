package com.bidcast.wallet_service.wallet;

import com.bidcast.wallet_service.core.exception.InvalidWalletOperationException;
import com.bidcast.wallet_service.core.exception.InsufficientWalletBalanceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;


import static org.junit.jupiter.api.Assertions.*;

class WalletTest {

    @Test
    @DisplayName("Debitar monto válido descuenta del saldo")
    void shouldDebitAmount() {
        Wallet wallet = Wallet.builder()
                .balance(new BigDecimal("100.00"))
                .build();

        wallet.debit(new BigDecimal("30.00"));

        assertTrue(new BigDecimal("70.00").compareTo(wallet.getBalance()) == 0);
    }

    @Test
    @DisplayName("Debitar más del saldo disponible lanza excepción")
    void shouldThrowExceptionWhenDebitInsufficientBalance() {
        Wallet wallet = Wallet.builder()
                .balance(new BigDecimal("10.00"))
                .build();

        assertThrows(InsufficientWalletBalanceException.class, () -> 
                wallet.debit(new BigDecimal("15.00")));
    }

    @Test
    @DisplayName("Congelar fondos mueve de saldo a saldo congelado")
    void shouldFreezeFunds() {
        Wallet wallet = Wallet.builder()
                .balance(new BigDecimal("100.00"))
                .frozenBalance(BigDecimal.ZERO)
                .build();

        wallet.freeze(new BigDecimal("40.00"));

        assertTrue(new BigDecimal("60.00").compareTo(wallet.getBalance()) == 0);
        assertTrue(new BigDecimal("40.00").compareTo(wallet.getFrozenBalance()) == 0);
    }

    @Test
    @DisplayName("Settle and Refund realiza la operación atómica correctamente")
    void shouldSettleAndRefundAtomically() {
        // GIVEN: Un presupuesto de 100 congelado previamente
        Wallet wallet = Wallet.builder()
                .balance(new BigDecimal("500.00")) // Saldo libre inicial
                .frozenBalance(new BigDecimal("100.00")) // 100 congelados para la puja
                .build();

        // WHEN: Se gastaron 40 de los 100
        wallet.settleAndRefund(new BigDecimal("40.00"), new BigDecimal("100.00"));

        // THEN:
        // El balance libre debería ser 500 + (100 - 40) = 560
        assertTrue(new BigDecimal("560.00").compareTo(wallet.getBalance()) == 0);
        // El balance congelado debería ser 0
        assertTrue(BigDecimal.ZERO.compareTo(wallet.getFrozenBalance()) == 0);
    }

    @Test
    @DisplayName("Settle and Refund lanza error si el totalFrozen es mayor al disponible")
    void shouldFailIfTotalFrozenIsGreaterThanActuallyFrozen() {
        Wallet wallet = Wallet.builder()
                .frozenBalance(new BigDecimal("50.00"))
                .build();

        assertThrows(InvalidWalletOperationException.class, () ->
                wallet.settleAndRefund(new BigDecimal("10.00"), new BigDecimal("100.00")));
    }

    @Test
    @DisplayName("Settle and Refund lanza error si el gasto es mayor al congelado")
    void shouldFailIfSpentIsGreaterThanTotalFrozen() {
        Wallet wallet = Wallet.builder()
                .frozenBalance(new BigDecimal("100.00"))
                .build();

        assertThrows(InvalidWalletOperationException.class, () ->
                wallet.settleAndRefund(new BigDecimal("120.00"), new BigDecimal("100.00")));
    }

    @Test
    @DisplayName("Settle and Refund falla rapido si spent es null")
    void shouldFailFastWhenSpentIsNull() {
        Wallet wallet = Wallet.builder()
                .frozenBalance(new BigDecimal("100.00"))
                .build();

        assertThrows(NullPointerException.class, () ->
                wallet.settleAndRefund(null, new BigDecimal("100.00")));
    }

    @Test
    @DisplayName("Settle and Refund falla rapido si totalFrozen es null")
    void shouldFailFastWhenTotalFrozenIsNull() {
        Wallet wallet = Wallet.builder()
                .frozenBalance(new BigDecimal("100.00"))
                .build();

        assertThrows(NullPointerException.class, () ->
                wallet.settleAndRefund(new BigDecimal("10.00"), null));
    }

    @Test
    @DisplayName("Acreditar monto no positivo lanza excepción de dominio")
    void shouldFailWhenCreditingNonPositiveAmount() {
        Wallet wallet = Wallet.builder()
                .balance(new BigDecimal("50.00"))
                .build();

        assertThrows(InvalidWalletOperationException.class, () -> wallet.credit(BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Descongelar más de lo congelado lanza excepción de dominio")
    void shouldFailWhenUnfreezingMoreThanFrozenBalance() {
        Wallet wallet = Wallet.builder()
                .balance(new BigDecimal("20.00"))
                .frozenBalance(new BigDecimal("10.00"))
                .build();

        assertThrows(InvalidWalletOperationException.class, () -> wallet.unfreeze(new BigDecimal("15.00")));
    }

    @Test
    @DisplayName("Liquidar monto no positivo lanza excepción de dominio")
    void shouldFailWhenSettlingNonPositiveAmount() {
        Wallet wallet = Wallet.builder()
                .frozenBalance(new BigDecimal("30.00"))
                .build();

        assertThrows(InvalidWalletOperationException.class, () -> wallet.settle(BigDecimal.ZERO));
    }
}
