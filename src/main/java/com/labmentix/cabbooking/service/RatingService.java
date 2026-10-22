package com.labmentix.cabbooking.service;

import com.labmentix.cabbooking.dto.RatingRequest;
import com.labmentix.cabbooking.dto.RatingResponse;
import com.labmentix.cabbooking.entity.Driver;
import com.labmentix.cabbooking.entity.Rating;
import com.labmentix.cabbooking.entity.Ride;
import com.labmentix.cabbooking.entity.User;
import com.labmentix.cabbooking.enums.RideStatus;
import com.labmentix.cabbooking.exception.BadRequestException;
import com.labmentix.cabbooking.exception.ResourceNotFoundException;
import com.labmentix.cabbooking.repository.DriverRepository;
import com.labmentix.cabbooking.repository.RatingRepository;
import com.labmentix.cabbooking.repository.RideRepository;
import com.labmentix.cabbooking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service managing user reviews, rating submissions, and driver reputation metrics.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RatingService {

    private final RatingRepository ratingRepository;
    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final DriverRepository driverRepository;

    @Transactional
    public RatingResponse submitRating(RatingRequest request, String userEmail) {
        log.info("Submitting rating for ride: {} by user: {}", request.getRideId(), userEmail);

        User ratedBy = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        Ride ride = rideRepository.findById(request.getRideId())
                .orElseThrow(() -> new ResourceNotFoundException("Ride", "id", request.getRideId()));

        if (ride.getStatus() != RideStatus.COMPLETED) {
            throw new BadRequestException("Ratings can only be submitted for completed rides");
        }

        if (ratingRepository.findByRide(ride).isPresent()) {
            throw new BadRequestException("A rating for this ride has already been submitted");
        }

        User ratedFor;
        if (ride.getRider().getId().equals(ratedBy.getId())) {
            // Rider is rating the Driver
            if (ride.getDriver() == null || ride.getDriver().getUser() == null) {
                throw new BadRequestException("No driver was assigned to this ride to rate");
            }
            ratedFor = ride.getDriver().getUser();
        } else if (ride.getDriver() != null && ride.getDriver().getUser().getId().equals(ratedBy.getId())) {
            // Driver is rating the Rider
            ratedFor = ride.getRider();
        } else {
            throw new BadRequestException("You were not an active participant in this ride");
        }

        Rating rating = Rating.builder()
                .ride(ride)
                .ratedBy(ratedBy)
                .ratedFor(ratedFor)
                .rating(request.getRating())
                .review(request.getReview())
                .build();

        Rating savedRating = ratingRepository.save(rating);

        // Update driver's average rating if the driver was the one rated
        Optional<Driver> driverOpt = driverRepository.findByUser(ratedFor);
        if (driverOpt.isPresent()) {
            Driver driver = driverOpt.get();
            Double avg = ratingRepository.findAverageRatingByRatedFor(ratedFor);
            if (avg != null) {
                double roundedAvg = BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP).doubleValue();
                driver.setAverageRating(roundedAvg);
                driverRepository.save(driver);
            }
        }

        return RatingResponse.fromEntity(savedRating);
    }

    @Transactional(readOnly = true)
    public List<RatingResponse> getRatingsForUser(UUID userId) {
        return ratingRepository.findByRatedForId(userId).stream()
                .map(RatingResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RatingResponse getRatingForRide(UUID rideId) {
        Rating rating = ratingRepository.findByRideId(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Rating for ride not found: " + rideId));
        return RatingResponse.fromEntity(rating);
    }
}
