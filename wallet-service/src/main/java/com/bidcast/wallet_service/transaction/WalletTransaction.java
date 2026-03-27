package com.bidcast.wallet_service.transaction;

import com.bidcast.wallet_service.wallet.Wallet;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "ledger_entries",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ledger_idempotency", columnNames = {"reference_id", "type"})
        },
        indexes = {
                @Index(name = "idx_ledger_wallet", columnList = "wallet_id"),
                @Index(name = "idx_ledger_reference", columnList = "reference_type, reference_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "wallet_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_transaction_wallet")
    )
    private Wallet wallet;

    @Column(name = "amount", nullable = false, precision = 12, scale = 4)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 12, scale = 4)
    private BigDecimal balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private WalletTransactionType type;

    @Column(name = "reference_type", nullable = false, length = 50)
    private String referenceType;

    @Column(name = "reference_id", nullable = false)
    private UUID referenceId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;

    public static WalletTransaction debitForProofOfPlay(
            Wallet wallet,
            BigDecimal amount,
            UUID proofOfPlayId
    ) {
        return WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount.negate())
                .balanceAfter(wallet.getBalance())
                .type(WalletTransactionType.POP_CHARGE_ADVERTISER_DEBIT)
                .referenceType("PROOF_OF_PLAY")
                .referenceId(proofOfPlayId)
                .build();
    }

    public static WalletTransaction creditForPublisher(
            Wallet wallet,
            BigDecimal amount,
            UUID proofOfPlayId
    ) {
        return WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount)
                .balanceAfter(wallet.getBalance())
                .type(WalletTransactionType.POP_PUBLISHER_CREDIT)
                .referenceType("PROOF_OF_PLAY")
                .referenceId(proofOfPlayId)
                .build();
    }

    public static WalletTransaction creditForPlatform(
            Wallet wallet,
            BigDecimal amount,
            UUID proofOfPlayId
    ) {
        return WalletTransaction.builder()
                .wallet(wallet)
                .amount(amount)
                .balanceAfter(wallet.getBalance())
                .type(WalletTransactionType.POP_PLATFORM_FEE)
                .referenceType("PROOF_OF_PLAY")
                .referenceId(proofOfPlayId)
                .build();
    }
}