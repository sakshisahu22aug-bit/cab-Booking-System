package com.labmentix.cabbooking.dto;

import com.labmentix.cabbooking.entity.Rating;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Rating response details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingResponse {

    private UUID ratingId;
    private UUID rideId;
    private Integer rating;
    private String review;
    private String riderName;
    private String driverName;
    private LocalDateTime createdAt;

    public static RatingResponse fromEntity(Rating rating) {
        if (rating == null) {
            return null;
        }

        RatingResponseBuilder builder = RatingResponse.builder()
                .ratingId(rating.getId())
                .rating(rating.getRating())
                .review(rating.getReview())
                .createdAt(rating.getCreatedAt());

        if (rating.getRide() != null) {
            builder.rideId(rating.getRide().getId());
            if (rating.getRide().getRider() != null) {
                builder.riderName(rating.getRide().getRider().getFullName());
            }
            if (rating.getRide().getDriver() != null && rating.getRide().getDriver().getUser() != null) {
                builder.driverName(rating.getRide().getDriver().getUser().getFullName());
            }
        }

        return builder.build();
    }
}
