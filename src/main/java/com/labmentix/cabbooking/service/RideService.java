package com.labmentix.cabbooking.service;

import com.labmentix.cabbooking.dto.FareEstimate;
import com.labmentix.cabbooking.dto.RideRequest;
import com.labmentix.cabbooking.dto.RideResponse;
import com.labmentix.cabbooking.entity.Driver;
import com.labmentix.cabbooking.entity.Ride;
import com.labmentix.cabbooking.entity.User;
import com.labmentix.cabbooking.enums.RideStatus;
import com.labmentix.cabbooking.enums.VehicleType;
import com.labmentix.cabbooking.exception.BadRequestException;
import com.labmentix.cabbooking.exception.ResourceNotFoundException;
import com.labmentix.cabbooking.exception.RideException;
import com.labmentix.cabbooking.repository.DriverRepository;
import com.labmentix.cabbooking.repository.RideRepository;
import com.labmentix.cabbooking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service managing the ride lifecycle from request, matching, dispatch, to completion.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RideService {

    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final EmailService emailService;

    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Calculate geodesic distance in kilometers between two GPS coordinates using Haversine formula.
     */
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return BigDecimal.valueOf(EARTH_RADIUS_KM * c)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * Compute fare, distance, and duration estimate.
     */
    public FareEstimate estimateFare(Double pickupLat, Double pickupLon, Double dropLat, Double dropLon, VehicleType vehicleType) {
        double distanceKm = calculateDistance(pickupLat, pickupLon, dropLat, dropLon);
        if (distanceKm < 0.1) {
            distanceKm = 0.5; // Minimum travel distance
        }

        // Estimate duration in minutes assuming average city driving speed of 30 km/h
        int estimatedMinutes = (int) Math.max(5, Math.ceil((distanceKm / 30.0) * 60));

        BigDecimal calculatedFare = vehicleType.getBaseFare()
                .add(vehicleType.getPerKmRate().multiply(BigDecimal.valueOf(distanceKm)))
                .setScale(2, RoundingMode.HALF_UP);

        return FareEstimate.builder()
                .distanceInKm(distanceKm)
                .estimatedTimeMinutes(estimatedMinutes)
                .estimatedFare(calculatedFare)
                .vehicleType(vehicleType)
                .build();
    }

    /**
     * Request a new ride.
     */
    @Transactional
    public RideResponse requestRide(RideRequest request, String riderEmail) {
        log.info("Requesting ride for rider email: {}", riderEmail);

        User rider = userRepository.findByEmail(riderEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", riderEmail));

        FareEstimate estimate = estimateFare(
                request.getPickupLatitude(),
                request.getPickupLongitude(),
                request.getDropLatitude(),
                request.getDropLongitude(),
                request.getVehicleType()
        );

        String otp = String.format("%04d", RANDOM.nextInt(10000));

        Ride ride = Ride.builder()
                .rider(rider)
                .pickupLatitude(request.getPickupLatitude())
                .pickupLongitude(request.getPickupLongitude())
                .dropLatitude(request.getDropLatitude())
                .dropLongitude(request.getDropLongitude())
                .pickupAddress(request.getPickupAddress())
                .dropAddress(request.getDropAddress())
                .fare(estimate.getEstimatedFare())
                .distanceInKm(estimate.getDistanceInKm())
                .estimatedTimeMinutes(estimate.getEstimatedTimeMinutes())
                .status(RideStatus.REQUESTED)
                .otp(otp)
                .build();

        Ride savedRide = rideRepository.save(ride);
        RideResponse response = RideResponse.fromEntity(savedRide);

        // Broadcast new ride notification to nearby active drivers
        messagingTemplate.convertAndSend("/topic/rides/requests", response);

        // Asynchronously dispatch OTP booking confirmation email to rider
        emailService.sendRideBookingEmail(rider.getEmail(), rider.getFullName(), savedRide.getPickupAddress(), otp);

        return response;
    }

    /**
     * Driver accepts an available ride.
     */
    @Transactional
    public RideResponse acceptRide(UUID rideId, UUID driverId) {
        log.info("Driver {} attempting to accept ride {}", driverId, rideId);

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride", "id", rideId));

        if (ride.getStatus() != RideStatus.REQUESTED) {
            throw new RideException("Ride is no longer available for booking. Current status: " + ride.getStatus());
        }

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", "id", driverId));

        if (!driver.isAvailable()) {
            throw new RideException("Driver is currently marked as unavailable or engaged in another ride");
        }

        ride.setDriver(driver);
        ride.setStatus(RideStatus.ACCEPTED);
        driver.setAvailable(false);

        driverRepository.save(driver);
        Ride savedRide = rideRepository.save(ride);
        RideResponse response = RideResponse.fromEntity(savedRide);

        // Notify rider and driver of ride acceptance
        messagingTemplate.convertAndSend("/topic/ride/" + rideId + "/status", response);

        return response;
    }

    /**
     * Driver notifies arrival at pickup location.
     */
    @Transactional
    public RideResponse driverArrived(UUID rideId, UUID driverId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride", "id", rideId));

        validateDriverRideOwnership(ride, driverId);

        if (ride.getStatus() != RideStatus.ACCEPTED) {
            throw new RideException("Cannot mark arrived for ride with status: " + ride.getStatus());
        }

        ride.setStatus(RideStatus.DRIVER_ARRIVED);
        Ride savedRide = rideRepository.save(ride);
        RideResponse response = RideResponse.fromEntity(savedRide);

        messagingTemplate.convertAndSend("/topic/ride/" + rideId + "/status", response);
        return response;
    }

    /**
     * Start ride after verifying 4-digit OTP provided by rider.
     */
    @Transactional
    public RideResponse startRide(UUID rideId, UUID driverId, String otp) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride", "id", rideId));

        validateDriverRideOwnership(ride, driverId);

        if (ride.getStatus() != RideStatus.DRIVER_ARRIVED && ride.getStatus() != RideStatus.ACCEPTED) {
            throw new RideException("Ride cannot be started from current status: " + ride.getStatus());
        }

        if (ride.getOtp() == null || !ride.getOtp().equals(otp.trim())) {
            throw new BadRequestException("Invalid ride verification OTP. Please verify with the rider.");
        }

        ride.setStatus(RideStatus.STARTED);
        ride.setStartTime(LocalDateTime.now());
        Ride savedRide = rideRepository.save(ride);
        RideResponse response = RideResponse.fromEntity(savedRide);

        messagingTemplate.convertAndSend("/topic/ride/" + rideId + "/status", response);
        return response;
    }

    /**
     * Complete ride when drop-off is reached.
     */
    @Transactional
    public RideResponse completeRide(UUID rideId, UUID driverId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride", "id", rideId));

        validateDriverRideOwnership(ride, driverId);

        if (ride.getStatus() != RideStatus.STARTED) {
            throw new RideException("Only started rides can be marked as completed");
        }

        ride.setStatus(RideStatus.COMPLETED);
        ride.setEndTime(LocalDateTime.now());

        Driver driver = ride.getDriver();
        if (driver != null) {
            driver.setAvailable(true);
            driver.setTotalRides(driver.getTotalRides() + 1);
            driverRepository.save(driver);
        }

        Ride savedRide = rideRepository.save(ride);
        RideResponse response = RideResponse.fromEntity(savedRide);

        messagingTemplate.convertAndSend("/topic/ride/" + rideId + "/status", response);
        return response;
    }

    /**
     * Cancel a ride.
     */
    @Transactional
    public RideResponse cancelRide(UUID rideId, String userEmail) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride", "id", rideId));

        if (ride.getStatus() == RideStatus.COMPLETED || ride.getStatus() == RideStatus.STARTED) {
            throw new RideException("Ride cannot be cancelled once it is in progress or completed");
        }

        ride.setStatus(RideStatus.CANCELLED);

        if (ride.getDriver() != null) {
            Driver driver = ride.getDriver();
            driver.setAvailable(true);
            driverRepository.save(driver);
        }

        Ride savedRide = rideRepository.save(ride);
        RideResponse response = RideResponse.fromEntity(savedRide);

        messagingTemplate.convertAndSend("/topic/ride/" + rideId + "/status", response);
        return response;
    }

    @Transactional(readOnly = true)
    public RideResponse getRideById(UUID rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride", "id", rideId));
        return RideResponse.fromEntity(ride);
    }

    @Transactional(readOnly = true)
    public List<RideResponse> getRiderRideHistory(UUID riderId) {
        return rideRepository.findByRiderIdOrderByCreatedAtDesc(riderId).stream()
                .map(RideResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RideResponse> getDriverRideHistory(UUID driverId) {
        return rideRepository.findByDriverIdOrderByCreatedAtDesc(driverId).stream()
                .map(RideResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private void validateDriverRideOwnership(Ride ride, UUID driverId) {
        if (ride.getDriver() == null || !ride.getDriver().getId().equals(driverId)) {
            throw new RideException("Driver is not assigned to this ride booking");
        }
    }
}
