package com.bidcast.auction_service.bid;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import lombok.AccessLevel;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "session_bids",
    indexes = {
        // indice para buscar rapido
        @Index(name = "idx_session_bid_lookup", columnList = "sessionId, status")
    }
)
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SessionBid {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    @NotBlank(message = "Session id is required")
    @Setter(AccessLevel.NONE)
    private String sessionId;

    @Column(nullable = false)
    @NotBlank(message = "Advertiser id is required")
    @Setter(AccessLevel.NONE)
    private String advertiserId;

    @Column(nullable = false)
    @NotBlank(message = "Campaign id is required")
    @Setter(AccessLevel.NONE)
    private String campaignId;

    @Column(nullable = false, precision = 19, scale = 4)
    @NotNull(message = "Total budget is required")
    @Positive(message = "Total budget must be positive")
    @Setter(AccessLevel.NONE)
    private BigDecimal totalBudget;

    @Column(nullable = false, precision = 19, scale = 4)
    @NotNull(message = "Advertiser bid price is required")
    @Positive(message = "Advertiser bid price must be positive")
    @Setter(AccessLevel.NONE)
    private BigDecimal advertiserBidPrice;

    @Column(nullable = false)
    @NotBlank(message = "Media URL is required")
    @Setter(AccessLevel.NONE)
    private String mediaUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Bid status is required")
    @Setter(AccessLevel.NONE)
    private BidStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Instant createdAt;

    @Version
    @Setter(AccessLevel.NONE)
    private Long version;

    /**
     * Activa la puja tras la reserva exitosa de fondos.
     */
    public void activate() {
        if (this.status != BidStatus.PENDING_RESERVATION) {
            throw new IllegalStateException("Can only activate bids in PENDING_RESERVATION status");
        }
        this.status = BidStatus.ACTIVE;
    }

    /**
     * Marca la puja como fallida durante el proceso de registro.
     */
    public void fail() {
        this.status = BidStatus.FAILED;
    }

    /**
     * Marca la puja con un fallo crítico de infraestructura.
     */
    public void markCriticalFailure() {
        this.status = BidStatus.FAILED_CRITICAL;
    }

    /**
     * Marca la puja como agotada (sin presupuesto).
     */
    public void exhaust() {
        this.status = BidStatus.EXHAUSTED;
    }

    /**
     * Cierra la puja al finalizar la sesión.
     */
    public void close() {
        this.status = BidStatus.CLOSED;
    }
}
