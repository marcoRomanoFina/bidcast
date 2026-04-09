package com.bidcast.selection_service.pop;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

// Entidad auditable de reproducción confirmada.
// Cada fila representa un hecho: "este playback efectivamente ocurrió".
@Entity
@Table(
    name = "proof_of_plays",
    indexes = {
        @Index(name = "idx_pop_offer_session", columnList = "offer_id, sessionId")
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

    // Session donde ocurrió la reproducción confirmada.
    @Column(nullable = false)
    @NotBlank(message = "Session ID is required")
    private String sessionId;

    // Offer que financió esa reproducción.
    @NotBlank(message = "Offer ID is required")
    @Column(name = "offer_id", nullable = false)
    private String offerId;

    // Advertiser asociado al receipt validado.
    @Column(nullable = false)
    @NotBlank(message = "Advertiser ID is required")
    private String advertiserId;

    // Monto real cobrado por esa reproducción.
    @Column(nullable = false, precision = 19, scale = 4)
    @NotNull(message = "Cost charged is required")
    @Positive(message = "Cost charged must be positive")
    private BigDecimal costCharged;

    // Receipt único firmado; también se usa para idempotencia persistente.
    @Column(nullable = false, unique = true)
    @NotBlank(message = "Play receipt ID is required")
    private String playReceiptId;

    // Momento exacto en que se persistió la confirmación.
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant playedAt;
}
