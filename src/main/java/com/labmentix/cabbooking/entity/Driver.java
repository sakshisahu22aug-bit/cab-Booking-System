package com.labmentix.cabbooking.entity;

import com.labmentix.cabbooking.enums.VehicleType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Driver entity storing driver credentials, vehicle details, location, and operational metrics.
 */
@Entity
@Table(name = "drivers", indexes = {
        @Index(name = "idx_drivers_license", columnList = "license_number", unique = true),
        @Index(name = "idx_drivers_vehicle_no", columnList = "vehicle_number", unique = true),
        @Index(name = "idx_drivers_availability", columnList = "available, verified"),
        @Index(name = "idx_drivers_location", columnList = "current_latitude, current_longitude")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "user")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id;

    @NotNull(message = "Associated user is required")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false, unique = true)
    private User user;

    @NotBlank(message = "License number is required")
    @Column(name = "license_number", nullable = false, unique = true)
    private String licenseNumber;

    @NotNull(message = "Vehicle type is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 20)
    private VehicleType vehicleType;

    @NotBlank(message = "Vehicle number is required")
    @Column(name = "vehicle_number", nullable = false, unique = true)
    private String vehicleNumber;

    @NotBlank(message = "Vehicle model is required")
    @Column(name = "vehicle_model", nullable = false)
    private String vehicleModel;

    @Column(name = "current_latitude")
    private Double currentLatitude;

    @Column(name = "current_longitude")
    private Double currentLongitude;

    @Builder.Default
    @Column(name = "available", nullable = false)
    private boolean available = true;

    @Builder.Default
    @Column(name = "verified", nullable = false)
    private boolean verified = false;

    @Builder.Default
    @Column(name = "average_rating", nullable = false)
    private double averageRating = 0.0;

    @Builder.Default
    @Column(name = "total_rides", nullable = false)
    private int totalRides = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
