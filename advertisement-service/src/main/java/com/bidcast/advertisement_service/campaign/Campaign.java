package com.bidcast.advertisement_service.campaign;

import jakarta.persistence.*;
import lombok.*;
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
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    
    @Column(nullable = false)
    private UUID advertiserId;

    
    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal budget;

    @Builder.Default
    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal spent = BigDecimal.ZERO; // todavia no lo uso

    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal bidCpm;

    private Instant startDate;

    private Instant endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CampaignStatusType status;

    @Builder.Default
    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Creative> creatives = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}