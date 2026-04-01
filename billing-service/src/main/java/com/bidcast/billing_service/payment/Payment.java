package com.bidcast.billing_service.payment;

import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.bidcast.billing_service.payment.event.PaymentConfirmedEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@Setter(AccessLevel.PROTECTED) 
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter(AccessLevel.NONE)
    private UUID id;

    @Version
    @Setter(AccessLevel.NONE)
    private Long version;

    @Column(nullable = false)
    private UUID advertiserId;

    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;


    @Column(unique = true)
    private String mpPreferenceId;

    @Column(unique = true)
    private String mpPaymentId;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    /**
     * Registra la preferencia generada en el gateway de pagos.
     */
    public void registerPreference(String mpPreferenceId) {
        if (this.mpPreferenceId != null) {
            throw new IllegalStateException("Payment preference already registered");
        }
        this.mpPreferenceId = mpPreferenceId;
    }

    /**
     * Marca el pago como aprobado tras la validación del webhook.
     * Devuelve true si el pago acaba de ser aprobado en esta llamada.
     */
    public boolean approve(String mpPaymentId) {
        if (this.status == PaymentStatus.APPROVED) {
            return false; // Ya estaba aprobado, no hubo cambios.
        }
        
        if (this.status == PaymentStatus.CANCELLED || this.status == PaymentStatus.REJECTED) {
            throw new IllegalStateException("Cannot approve a payment that is already " + this.status);
        }

        this.status = PaymentStatus.APPROVED;
        this.mpPaymentId = mpPaymentId;
        return true; // Estado actualizado correctamente.
    }

    /**
     * Crea el evento de dominio de confirmación de pago.
     * Encapsula la creación del evento dentro de la entidad.
     */
    public com.bidcast.billing_service.payment.event.PaymentConfirmedEvent exportConfirmedEvent() {
        if (this.status != PaymentStatus.APPROVED) {
            throw new IllegalStateException("Can only export event for approved payments");
        }
        return new PaymentConfirmedEvent(
                this.id,
                this.advertiserId,
                this.amount
        );
    }
}
