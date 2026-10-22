package com.labmentix.cabbooking.entity;

import com.labmentix.cabbooking.enums.PaymentMethod;
import com.labmentix.cabbooking.enums.PaymentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Payment entity recording transaction information for a completed or active ride.
 */
@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payments_ride", columnList = "ride_id", unique = true),
        @Index(name = "idx_payments_rider", columnList = "rider_id"),
        @Index(name = "idx_payments_stripe_intent", columnList = "stripe_payment_intent_id"),
        @Index(name = "idx_payments_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"ride", "rider"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id;

    @NotNull(message = "Ride reference is required")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id", referencedColumnName = "id", nullable = false, unique = true)
    private Ride ride;

    @NotNull(message = "Rider reference is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rider_id", referencedColumnName = "id", nullable = false)
    private User rider;

    @NotNull(message = "Payment amount is required")
    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Builder.Default
    @Column(name = "currency", nullable = false, length = 10)
    private String currency = "INR";

    @NotNull(message = "Payment method is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private PaymentMethod paymentMethod;

    @Column(name = "stripe_payment_intent_id")
    private String stripePaymentIntentId;

    @NotNull(message = "Payment status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PaymentStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.currency == null || this.currency.isBlank()) {
            this.currency = "INR";
        }
        if (this.status == null) {
            this.status = PaymentStatus.PENDING;
        }
    }
}
