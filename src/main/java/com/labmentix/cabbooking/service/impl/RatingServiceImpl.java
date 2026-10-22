package com.labmentix.cabbooking.service.impl;

import com.labmentix.cabbooking.dto.RatingRequest;
import com.labmentix.cabbooking.dto.RatingResponse;
import com.labmentix.cabbooking.entity.*;
import com.labmentix.cabbooking.enums.*;
import com.labmentix.cabbooking.exception.BadRequestException;
import com.labmentix.cabbooking.exception.ResourceNotFoundException;
import com.labmentix.cabbooking.repository.DriverRepository;
import com.labmentix.cabbooking.repository.RatingRepository;
import com.labmentix.cabbooking.repository.RideRepository;
import com.labmentix.cabbooking.repository.UserRepository;
import com.labmentix.cabbooking.service.RatingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of RatingService for managing ride feedback and calculating driver ratings.
 */
@Service
@Transactional
public class RatingServiceImpl implements RatingService {

    private static final Logger log = LoggerFactory.getLogger(RatingServiceImpl.class);

    private final RatingRepository ratingRepository;
    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final DriverRepository driverRepository;

    @Autowired
    public RatingServiceImpl(RatingRepository ratingRepository,
                             RideRepository rideRepository,
                             UserRepository userRepository,
                             DriverRepository driverRepository) {
        this.ratingRepository = ratingRepository;
        this.rideRepository = rideRepository;
        this.userRepository = userRepository;
        this.driverRepository = driverRepository;
    }

    @Override
    public RatingResponse rateRide(RatingRequest request, String userEmail) {
        log.info("Submitting rating for ride: {} by user: {}", request.getRideId(), userEmail);

        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new BadRequestException("Rating score must be between 1 and 5 stars");
        }

        User ratedBy = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        Ride ride = rideRepository.findById(request.getRideId())
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with ID: " + request.getRideId()));

        if (ride.getStatus() != RideStatus.COMPLETED) {
            throw new BadRequestException("Only completed rides can be rated. Current status: " + ride.getStatus());
        }

        boolean isRider = ride.getRider() != null && ratedBy.getId().equals(ride.getRider().getId());
        boolean isDriver = ride.getDriver() != null && ride.getDriver().getUser() != null
                && ratedBy.getId().equals(ride.getDriver().getUser().getId());

        if (!isRider && !isDriver) {
            throw new BadRequestException("You were not a participant in this ride");
        }

        // Determine who is being rated
        User ratedFor;
        if (isRider) {
            if (ride.getDriver() == null || ride.getDriver().getUser() == null) {
                throw new BadRequestException("Assigned driver is missing for this ride");
            }
            ratedFor = ride.getDriver().getUser();
        } else {
            if (ride.getRider() == null) {
                throw new BadRequestException("Rider information is missing for this ride");
            }
            ratedFor = ride.getRider();
        }

        if (ratingRepository.existsByRideAndRatedBy(ride, ratedBy)) {
            throw new BadRequestException("You have already submitted a rating for this ride");
        }

        Rating rating = new Rating();
        rating.setRide(ride);
        rating.setRatedBy(ratedBy);
        rating.setRatedFor(ratedFor);
        rating.setRating(request.getRating());
        rating.setReview(request.getReview());
        rating.setCreatedAt(LocalDateTime.now());

        Rating savedRating = ratingRepository.save(rating);
        log.info("Rating saved with ID: {}", savedRating.getId());

        // If the rating is for a driver, recalculate and update driver's average rating
        if (isRider && ride.getDriver() != null) {
            Driver driver = ride.getDriver();
            Double avgRating = ratingRepository.findAverageRatingByRatedFor(ratedFor);
            if (avgRating == null) {
                List<Rating> driverRatings = ratingRepository.findByRatedFor(ratedFor);
                avgRating = driverRatings.stream()
                        .mapToInt(Rating::getRating)
                        .average()
                        .orElse(request.getRating());
            }

            double roundedAvg = Math.round(avgRating * 100.0) / 100.0;
            driver.setAverageRating(roundedAvg);
            driver.setUpdatedAt(LocalDateTime.now());
            driverRepository.save(driver);
            log.info("Updated average rating for driver {} to {}", driver.getId(), roundedAvg);
        }

        return mapToRatingResponse(savedRating);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RatingResponse> getRideRatings(UUID rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with ID: " + rideId));

        return ratingRepository.findByRide(ride).stream()
                .map(this::mapToRatingResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RatingResponse> getDriverRatings(UUID driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with ID: " + driverId));

        if (driver.getUser() == null) {
            throw new ResourceNotFoundException("User profile not linked to driver: " + driverId);
        }

        return ratingRepository.findByRatedFor(driver.getUser()).stream()
                .map(this::mapToRatingResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Double getDriverAverageRating(UUID driverId) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with ID: " + driverId));

        return driver.getAverageRating() != null ? driver.getAverageRating() : 0.0;
    }

    private RatingResponse mapToRatingResponse(Rating rating) {
        if (rating == null) {
            return null;
        }

        RatingResponse response = new RatingResponse();
        response.setId(rating.getId());
        if (rating.getRide() != null) {
            response.setRideId(rating.getRide().getId());
        }
        if (rating.getRatedBy() != null) {
            response.setRatedById(rating.getRatedBy().getId());
            response.setRatedByName(rating.getRatedBy().getFullName());
        }
        if (rating.getRatedFor() != null) {
            response.setRatedForId(rating.getRatedFor().getId());
            response.setRatedForName(rating.getRatedFor().getFullName());
        }
        response.setRating(rating.getRating());
        response.setReview(rating.getReview());
        response.setCreatedAt(rating.getCreatedAt());

        return response;
    }
}
