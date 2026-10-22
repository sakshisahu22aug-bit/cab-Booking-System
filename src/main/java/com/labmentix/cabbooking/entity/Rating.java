package com.labmentix.cabbooking.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Rating entity recording feedback and numeric ratings between riders and drivers.
 */
@Entity
@Table(name = "ratings", indexes = {
        @Index(name = "idx_ratings_ride", columnList = "ride_id", unique = true),
        @Index(name = "idx_ratings_rated_by", columnList = "rated_by_id"),
        @Index(name = "idx_ratings_rated_for", columnList = "rated_for_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"ride", "ratedBy", "ratedFor"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private UUID id;

    @NotNull(message = "Ride reference is required")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ride_id", referencedColumnName = "id", nullable = false, unique = true)
    private Ride ride;

    @NotNull(message = "Rated-by user is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rated_by_id", referencedColumnName = "id", nullable = false)
    private User ratedBy;

    @NotNull(message = "Rated-for user is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rated_for_id", referencedColumnName = "id", nullable = false)
    private User ratedFor;

    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating cannot exceed 5")
    @Column(name = "rating", nullable = false)
    private int rating;

    @Column(name = "review", length = 1000)
    private String review;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
