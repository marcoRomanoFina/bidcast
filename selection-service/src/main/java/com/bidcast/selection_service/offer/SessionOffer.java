package com.bidcast.selection_service.offer;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;
import lombok.AccessLevel;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
    name = "session_offers",
    indexes = {
        // Lookup principal: traer offers de una session por estado operativo.
        @Index(name = "idx_session_offer_lookup", columnList = "sessionId, status")
    }
)
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
// SessionOffer es la entrada económica de una campaign dentro de una session.
//
// - una campaign entra una sola vez a competir
// - esa entrada tiene presupuesto, precio por slot y varios creatives snapshot
// - el engine elige la offer y luego rota creatives dentro de ella

public class SessionOffer {

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
    @NotNull(message = "Price per slot is required")
    @Positive(message = "Price per slot must be positive")
    @Setter(AccessLevel.NONE)
    private BigDecimal pricePerSlot;

    @Column(name = "device_cooldown_seconds", nullable = false)
    @NotNull(message = "Device cooldown is required")
    @Positive(message = "Device cooldown must be positive")
    @Setter(AccessLevel.NONE)
    private Integer deviceCooldownSeconds;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "session_offer_creatives",
        joinColumns = @JoinColumn(name = "session_offer_id"),
        indexes = {
            @Index(name = "idx_session_offer_creatives_offer_id", columnList = "session_offer_id")
        }
    )
    @OrderColumn(name = "creative_order")
    @Builder.Default
    @NotEmpty(message = "At least one creative snapshot is required")
    private List<CreativeSnapshot> creatives = new ArrayList<>();

    @Column(name = "next_creative_index", nullable = false)
    @Builder.Default
    private Integer nextCreativeIndex = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull(message = "Offer status is required")
    @Setter(AccessLevel.NONE)
    private OfferStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    @Setter(AccessLevel.NONE)
    private Instant createdAt;

    @Version
    @Setter(AccessLevel.NONE)
    private Long version;

    /**
     * Activa la offer después de reservar correctamente sus fondos iniciales.
     */
    public void activate() {
        if (this.status != OfferStatus.PENDING_RESERVATION) {
            throw new IllegalStateException("Can only activate offers in PENDING_RESERVATION status");
        }
        this.status = OfferStatus.ACTIVE;
    }

    /**
     * Marca una falla de negocio o de alta.
     */
    public void fail() {
        this.status = OfferStatus.FAILED;
    }

    /**
     * Marca una falla crítica de infraestructura.
     */
    public void markCriticalFailure() {
        this.status = OfferStatus.FAILED_CRITICAL;
    }

    /**
     * Marca que la offer ya no tiene saldo suficiente para financiar más reproducciones.
     */
    public void exhaust() {
        this.status = OfferStatus.EXHAUSTED;
    }

    /**
     * Cierra definitivamente la offer cuando la session terminó.
     */
    public void close() {
        this.status = OfferStatus.CLOSED;
    }

    /**
     * Devuelve el creative al que hoy apunta el puntero, sin modificar estado.
     */
    public Optional<CreativeSnapshot> currentCreative() {
        if (creatives == null || creatives.isEmpty()) {
            return Optional.empty();
        }

        int safeIndex = Math.floorMod(nextCreativeIndex == null ? 0 : nextCreativeIndex, creatives.size());
        return Optional.of(creatives.get(safeIndex));
    }

    /**
     * Busca el próximo creative elegible sin mover el puntero.
     *
     * Esto sirve cuando primero querés mirar candidatos y recién avanzar el índice
     * si finalmente ese creative termina siendo seleccionado.
     */
    public Optional<CreativeSnapshot> nextEligibleCreative(Set<String> excludedCreativeIds) {
        if (creatives == null || creatives.isEmpty()) {
            return Optional.empty();
        }

        Set<String> excluded = excludedCreativeIds == null ? Set.of() : excludedCreativeIds;
        int startIndex = Math.floorMod(nextCreativeIndex == null ? 0 : nextCreativeIndex, creatives.size());

        for (int offset = 0; offset < creatives.size(); offset++) {
            int candidateIndex = (startIndex + offset) % creatives.size();
            CreativeSnapshot candidate = creatives.get(candidateIndex);

            if (!excluded.contains(candidate.creativeId())) {
                return Optional.of(candidate);
            }
        }

        return Optional.empty();
    }

    /**
     * Avanza el puntero circular una posición y devuelve el creative previo.
     */
    public Optional<CreativeSnapshot> advanceCreativePointer() {
        if (creatives == null || creatives.isEmpty()) {
            return Optional.empty();
        }

        int currentIndex = Math.floorMod(nextCreativeIndex == null ? 0 : nextCreativeIndex, creatives.size());
        CreativeSnapshot selected = creatives.get(currentIndex);
        nextCreativeIndex = (currentIndex + 1) % creatives.size();
        return Optional.of(selected);
    }

    /**
     * Recorre circularmente hasta encontrar un creative no excluido y deja el
     * puntero parado después de ese creative.
     */
    public Optional<CreativeSnapshot> advanceToNextEligibleCreative(Set<String> excludedCreativeIds) {
        if (creatives == null || creatives.isEmpty()) {
            return Optional.empty();
        }

        Set<String> excluded = excludedCreativeIds == null ? Set.of() : excludedCreativeIds;
        int startIndex = Math.floorMod(nextCreativeIndex == null ? 0 : nextCreativeIndex, creatives.size());

        for (int offset = 0; offset < creatives.size(); offset++) {
            int candidateIndex = (startIndex + offset) % creatives.size();
            CreativeSnapshot candidate = creatives.get(candidateIndex);

            if (!excluded.contains(candidate.creativeId())) {
                nextCreativeIndex = (candidateIndex + 1) % creatives.size();
                return Optional.of(candidate);
            }
        }

        return Optional.empty();
    }

    /**
     * Mueve el puntero como si el creative indicado hubiese sido elegido.
     *
     * Se usa cuando el scoring ya decidió un creative concreto y queremos dejar
     * persistido el siguiente punto de rotación.
     */
    public Optional<CreativeSnapshot> advanceToCreative(String creativeId) {
        if (creatives == null || creatives.isEmpty() || creativeId == null || creativeId.isBlank()) {
            return Optional.empty();
        }

        for (int i = 0; i < creatives.size(); i++) {
            CreativeSnapshot candidate = creatives.get(i);
            if (creativeId.equals(candidate.creativeId())) {
                nextCreativeIndex = (i + 1) % creatives.size();
                return Optional.of(candidate);
            }
        }

        return Optional.empty();
    }

    /**
     * Devuelve el menor slotCount de la offer.
     *
     * Como pricePerSlot es constante por offer, esto alcanza para saber cuál es
     * el costo mínimo posible de reproducir cualquiera de sus creatives.
     */
    public OptionalLong minimumSlotCount() {
        if (creatives == null || creatives.isEmpty()) {
            return OptionalLong.empty();
        }

        return creatives.stream()
                .mapToLong(CreativeSnapshot::slotCount)
                .min();
    }
}
