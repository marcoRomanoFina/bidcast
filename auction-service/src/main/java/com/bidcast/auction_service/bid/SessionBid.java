package com.bidcast.auction_service.bid;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "session_bids",
    indexes = {
        @Index(name = "idx_session_bid_lookup", columnList = "sessionId, status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionBid {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    @NotBlank(message = "Session ID es obligatorio")
    private String sessionId;

    @Column(nullable = false)
    @NotBlank(message = "Advertiser ID es obligatorio")
    private String advertiserId;

    @Column(nullable = false)
    @NotBlank(message = "Campaign ID es obligatorio")
    private String campaignId;

    @Column(nullable = false, precision = 19, scale = 4)
    @NotNull(message = "El presupuesto total es obligatorio")
    @Positive(message = "El presupuesto total debe ser positivo")
    private BigDecimal totalBudget;

    @Column(nullable = false, precision = 19, scale = 4)
    @NotNull(message = "El precio de puja del anunciante es obligatorio")
    @Positive(message = "El precio de puja debe ser positivo")
    private BigDecimal advertiserBidPrice;

    @Column(nullable = false)
    @NotBlank(message = "Media URL is required")
    private String mediaUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Bid status is required")
    private BidStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Version
    private Long version;
}
