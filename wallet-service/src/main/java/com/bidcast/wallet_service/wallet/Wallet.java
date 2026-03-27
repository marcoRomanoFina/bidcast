package com.bidcast.wallet_service.wallet;

import com.bidcast.wallet_service.core.exception.InvalidWalletOperationException;
import com.bidcast.wallet_service.core.exception.InsufficientWalletBalanceException;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "wallets", uniqueConstraints = {
        @UniqueConstraint(name = "uk_wallet_owner", columnNames = { "owner_id", "owner_type" })
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "Wallet", description = "Current wallet state within the ledger")
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Schema(description = "Internal wallet identifier", example = "37f2a2bf-a4da-4ca5-b867-f8c654f5b1b0")
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    @Schema(description = "Wallet owner identifier", example = "2b9fd6d4-ef58-4d56-9aa2-8d6f72d5ce59")
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 20)
    @Schema(description = "Wallet owner type", example = "ADVERTISER")
    private WalletOwnerType ownerType;

    @Column(name = "currency_code", nullable = false, length = 3)
    @Schema(description = "ISO currency code", example = "ARS")
    private String currencyCode;

    @Column(
            name = "balance", 
            nullable = false, 
            precision = 12, 
            scale = 4, 
            columnDefinition = "DECIMAL(12,4) CHECK (balance >= 0)"
    )
    @Builder.Default
    @Schema(description = "Available balance", example = "1060.0000")
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(
            name = "frozen_balance", 
            nullable = false, 
            precision = 12, 
            scale = 4, 
            columnDefinition = "DECIMAL(12,4) CHECK (frozen_balance >= 0)"
    )
    @Builder.Default
    @Schema(description = "Reserved or frozen balance for pending operations", example = "40.0000")
    private BigDecimal frozenBalance = BigDecimal.ZERO;

    @Version
    @Column(name = "version", nullable = false)
    @Schema(description = "Optimistic locking version", example = "3")
    private long version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    @Schema(description = "Wallet creation timestamp", example = "2026-03-18T23:20:17Z")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @Schema(description = "Last wallet update timestamp", example = "2026-03-18T23:25:10Z")
    private Instant updatedAt;

    public void debit(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidWalletOperationException("Debit amount must be positive");
        }
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientWalletBalanceException(id);
        }
        balance = balance.subtract(amount);
    }

    public void freeze(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidWalletOperationException("Freeze amount must be positive");
        }
        if (balance.compareTo(amount) < 0) {
            throw new InsufficientWalletBalanceException(id);
        }
        balance = balance.subtract(amount);
        frozenBalance = frozenBalance.add(amount);
    }

    public void unfreeze(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidWalletOperationException("Unfreeze amount must be positive");
        }
        if (frozenBalance.compareTo(amount) < 0) {
            throw new InvalidWalletOperationException("Insufficient frozen balance");
        }
        frozenBalance = frozenBalance.subtract(amount);
        balance = balance.add(amount);
    }

    public void settle(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidWalletOperationException("Settlement amount must be positive");
        }
        if (frozenBalance.compareTo(amount) < 0) {
            throw new InvalidWalletOperationException("Insufficient frozen balance for settlement");
        }
        frozenBalance = frozenBalance.subtract(amount);
    }

    /**
     * Realiza una liquidación final (settle) y devuelve el sobrante (unfreeze) en un solo paso atómico.
     * Ideal para cierre de sesiones de puja donde se congeló un presupuesto máximo.
     * @param spent El monto realmente gastado.
     * @param totalFrozen El monto total que estaba congelado para esta operación.
     */
    public void settleAndRefund(BigDecimal spent, BigDecimal totalFrozen) {
        Objects.requireNonNull(spent, "spent no puede ser null");
        Objects.requireNonNull(totalFrozen, "totalFrozen no puede ser null");

        if (spent.signum() < 0) {
            throw new InvalidWalletOperationException("Spent amount must be zero or positive");
        }
        if (totalFrozen.signum() <= 0) {
            throw new InvalidWalletOperationException("Total frozen amount must be positive");
        }
        if (frozenBalance.compareTo(totalFrozen) < 0) {
            throw new InvalidWalletOperationException("Insufficient frozen balance to settle and refund");
        }
        if (spent.compareTo(totalFrozen) > 0) {
            throw new InvalidWalletOperationException("Spent amount cannot be greater than total frozen");
        }

        BigDecimal refund = totalFrozen.subtract(spent);
        
        // 1. Descontamos el total congelado del balde de 'frozen'
        this.frozenBalance = this.frozenBalance.subtract(totalFrozen);
        
        // 2. Devolvemos el sobrante al balde de 'disponible'
        this.balance = this.balance.add(refund);
        
        // El 'spent' simplemente desaparece del sistema (se movió a otras billeteras o es fee)
    }

    public void credit(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new InvalidWalletOperationException("Credit amount must be positive");
        }
        balance = balance.add(amount);
    }
}
