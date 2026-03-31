package com.bidcast.advertisement_service.campaign;

import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel; // Import faltante
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "campaigns")
@Getter
@Setter(AccessLevel.PROTECTED) 
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor // Visible para el Builder
@Builder
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter(AccessLevel.NONE)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    
    @Column(nullable = false)
    @Setter(AccessLevel.NONE)
    private UUID advertiserId;

    
    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal budget;

    @Builder.Default
    @Column(nullable = false, precision = 12, scale = 4)
    @Setter(AccessLevel.NONE)
    private BigDecimal spent = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal bidCpm;

    private Instant startDate;

    private Instant endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Setter(AccessLevel.NONE)
    private CampaignStatusType status;

    @Builder.Default
    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    @Setter(AccessLevel.NONE)
    private List<Creative> creatives = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    /**
     * Activa una campaña que está en borrador o programada.
     */
    public void activate() {
        if (creatives.isEmpty()) {
            throw new IllegalStateException("Cannot activate a campaign without creatives");
        }
        if (status == CampaignStatusType.FINISHED) {
            throw new IllegalStateException("Cannot reactivate a finished campaign");
        }
        status = CampaignStatusType.ACTIVE;
    }

    /**
     * Pausa una campaña activa.
     */
    public void pause() {
        if (this.status != CampaignStatusType.ACTIVE) {
            throw new IllegalStateException("Can only pause active campaigns");
        }
        this.status = CampaignStatusType.PAUSED;
    }

    /**
     * Reanuda una campaña pausada.
     */
    public void resume() {
        if (this.status != CampaignStatusType.PAUSED) {
            throw new IllegalStateException("Can only resume paused campaigns");
        }
        this.status = CampaignStatusType.ACTIVE;
    }

    /**
     * Finaliza la campaña.
     */
    public void finish() {
        this.status = CampaignStatusType.FINISHED;
    }

    /**
     * Registra un gasto en la campaña y comprueba si ha finalizado el presupuesto.
     */
    public void registerSpend(BigDecimal amount) {
        this.spent = this.spent.add(amount);
        if (this.spent.compareTo(this.budget) >= 0) {
            this.finish();
        }
    }
}