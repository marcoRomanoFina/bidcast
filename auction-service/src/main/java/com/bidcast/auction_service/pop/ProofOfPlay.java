package com.bidcast.auction_service.pop;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;


// entidad que representa el PoP
@Entity
@Table(
    name = "proof_of_plays",
    indexes = {
        @Index(name = "idx_pop_bid_session", columnList = "bidId, sessionId"),
        @Index(name = "idx_pop_budget_sum", columnList = "bidId, costCharged")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProofOfPlay {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    @NotBlank(message = "Session ID is required")
    private String sessionId;

    @Column(nullable = false)
    @NotBlank(message = "Bid ID is required")
    private String bidId;

    @Column(nullable = false)
    @NotBlank(message = "Advertiser ID is required")
    private String advertiserId;

    @Column(nullable = false, precision = 19, scale = 4)
    @NotNull(message = "Cost charged is required")
    @Positive(message = "Cost charged must be positive")
    private BigDecimal costCharged;

    @Column(nullable = false, unique = true)
    @NotBlank(message = "Play receipt ID is required")
    private String playReceiptId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant playedAt;
}
