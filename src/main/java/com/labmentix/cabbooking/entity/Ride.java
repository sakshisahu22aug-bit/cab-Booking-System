package com.labmentix.cabbooking.entity;

import com.labmentix.cabbooking.enums.RideStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Ride entity representing a booking request from creation to completion or cancellation.
 */
@Entity
@Table(name = "rides", indexes = {
        @Index(name = "idx_rides_rider", columnList = "rider_id"),
        @Index(name = "idx_rides_driver", columnList = "driver_id"),
        @Index(name = "idx_rides_status", columnList = "status"),
        @Index(name = "idx_rides_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"rider", "driver"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id;

    @NotNull(message = "Rider is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rider_id", nullable = false)
    private User rider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @NotNull(message = "Pickup latitude is required")
    @Column(name = "pickup_latitude", nullable = false)
    private Double pickupLatitude;

    @NotNull(message = "Pickup longitude is required")
    @Column(name = "pickup_longitude", nullable = false)
    private Double pickupLongitude;

    @NotNull(message = "Drop latitude is required")
    @Column(name = "drop_latitude", nullable = false)
    private Double dropLatitude;

    @NotNull(message = "Drop longitude is required")
    @Column(name = "drop_longitude", nullable = false)
    private Double dropLongitude;

    @NotBlank(message = "Pickup address is required")
    @Column(name = "pickup_address", nullable = false, length = 500)
    private String pickupAddress;

    @NotBlank(message = "Drop address is required")
    @Column(name = "drop_address", nullable = false, length = 500)
    private String dropAddress;

    @NotNull(message = "Ride status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private RideStatus status;

    @NotNull(message = "Fare is required")
    @Column(name = "fare", nullable = false, precision = 10, scale = 2)
    private BigDecimal fare;

    @NotNull(message = "Distance is required")
    @Column(name = "distance_in_km", nullable = false)
    private Double distanceInKm;

    @NotNull(message = "Estimated time is required")
    @Column(name = "estimated_time_minutes", nullable = false)
    private Integer estimatedTimeMinutes;

    @Column(name = "otp", length = 4)
    private String otp;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = RideStatus.REQUESTED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
