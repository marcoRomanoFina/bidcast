package com.bidcast.wallet_service.charge;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "proof_of_play_charges", uniqueConstraints = {
                @UniqueConstraint(name = "uk_proof_of_play_id", columnNames = "proof_of_play_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProofOfPlayCharge {

        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        private UUID id;

        @Column(name = "proof_of_play_id", nullable = false, updatable = false)
        private UUID proofOfPlayId;

        @Column(name = "gross_amount", nullable = false, precision = 12, scale = 4)
        private BigDecimal grossAmount;

        @Column(name = "publisher_amount", nullable = false, precision = 12, scale = 4)
        private BigDecimal publisherAmount;

        @Column(name = "platform_fee_amount", nullable = false, precision = 12, scale = 4)
        private BigDecimal platformFeeAmount;

        @Column(name = "advertiser_wallet_id", nullable = false, updatable = false)
        private UUID advertiserWalletId;

        @Column(name = "publisher_wallet_id", nullable = false, updatable = false)
        private UUID publisherWalletId;

        @Column(name = "platform_wallet_id", nullable = false, updatable = false)
        private UUID platformWalletId;

        @CreationTimestamp
        @Column(name = "created_at", updatable = false, nullable = false)
        private Instant createdAt;
}