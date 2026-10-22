package com.labmentix.cabbooking.service.impl;

import com.labmentix.cabbooking.dto.FareEstimate;
import com.labmentix.cabbooking.dto.RideRequest;
import com.labmentix.cabbooking.dto.RideResponse;
import com.labmentix.cabbooking.entity.*;
import com.labmentix.cabbooking.enums.*;
import com.labmentix.cabbooking.exception.BadRequestException;
import com.labmentix.cabbooking.exception.ResourceNotFoundException;
import com.labmentix.cabbooking.exception.RideException;
import com.labmentix.cabbooking.repository.DriverRepository;
import com.labmentix.cabbooking.repository.RideRepository;
import com.labmentix.cabbooking.repository.UserRepository;
import com.labmentix.cabbooking.service.GeoLocationService;
import com.labmentix.cabbooking.service.NotificationService;
import com.labmentix.cabbooking.service.RideService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
 * Implementation of RideService containing all core business logic for the ride lifecycle.
 */
@Service
@Transactional
public class RideServiceImpl implements RideService {

    private static final Logger log = LoggerFactory.getLogger(RideServiceImpl.class);
    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double NEARBY_DRIVER_RADIUS_KM = 5.0;

    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final NotificationService notificationService;
    private final GeoLocationService geoLocationService;

    @Autowired
    public RideServiceImpl(RideRepository rideRepository,
                           UserRepository userRepository,
                           DriverRepository driverRepository,
                           NotificationService notificationService,
                           @Autowired(required = false) GeoLocationService geoLocationService) {
        this.rideRepository = rideRepository;
        this.userRepository = userRepository;
        this.driverRepository = driverRepository;
        this.notificationService = notificationService;
        this.geoLocationService = geoLocationService;
    }

    @Override
    public RideResponse requestRide(RideRequest request, String riderEmail) {
        log.info("Processing ride request for rider: {}", riderEmail);

        User rider = userRepository.findByEmail(riderEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Rider not found with email: " + riderEmail));

        VehicleType vehicleType = request.getVehicleType() != null ? request.getVehicleType() : VehicleType.SEDAN;

        // Calculate distance via GeoLocationService if available, fallback to Haversine
        double distanceInKm;
        if (geoLocationService != null) {
            try {
                distanceInKm = geoLocationService.calculateDistance(
                        request.getPickupLatitude(), request.getPickupLongitude(),
                        request.getDropLatitude(), request.getDropLongitude()
                );
            } catch (Exception e) {
                log.warn("GeoLocationService failed to calculate distance, falling back to Haversine: {}", e.getMessage());
                distanceInKm = calculateHaversineDistance(
                        request.getPickupLatitude(), request.getPickupLongitude(),
                        request.getDropLatitude(), request.getDropLongitude()
                );
            }
        } else {
            distanceInKm = calculateHaversineDistance(
                    request.getPickupLatitude(), request.getPickupLongitude(),
                    request.getDropLatitude(), request.getDropLongitude()
            );
        }

        FareEstimate estimate = estimateFare(
                request.getPickupLatitude(), request.getPickupLongitude(),
                request.getDropLatitude(), request.getDropLongitude(),
                vehicleType
        );

        String otp = generateOtp();

        Ride ride = new Ride();
        ride.setRider(rider);
        ride.setPickupLatitude(request.getPickupLatitude());
        ride.setPickupLongitude(request.getPickupLongitude());
        ride.setDropLatitude(request.getDropLatitude());
        ride.setDropLongitude(request.getDropLongitude());
        ride.setPickupAddress(request.getPickupAddress());
        ride.setDropAddress(request.getDropAddress());
        ride.setStatus(RideStatus.REQUESTED);
        ride.setFare(estimate.getEstimatedFare());
        ride.setDistanceInKm(distanceInKm);
        ride.setEstimatedTimeMinutes(estimate.getEstimatedTimeMinutes());
        ride.setOtp(otp);
        ride.setCreatedAt(LocalDateTime.now());
        ride.setUpdatedAt(LocalDateTime.now());

        Ride savedRide = rideRepository.save(ride);
        log.info("Ride requested successfully with ID: {}, OTP: {}", savedRide.getId(), otp);

        // Find nearby drivers within 5km radius and broadcast ride request
        try {
            List<Driver> availableDrivers = driverRepository.findByAvailableTrueAndVerifiedTrue();
            List<Driver> nearbyDrivers = availableDrivers.stream()
                    .filter(d -> d.getCurrentLatitude() != null && d.getCurrentLongitude() != null)
                    .filter(d -> calculateHaversineDistance(
                            request.getPickupLatitude(), request.getPickupLongitude(),
                            d.getCurrentLatitude(), d.getCurrentLongitude()) <= NEARBY_DRIVER_RADIUS_KM)
                    .collect(Collectors.toList());

            log.info("Found {} nearby available drivers for ride {}", nearbyDrivers.size(), savedRide.getId());
            notificationService.sendRideStatusUpdate(savedRide.getId(), RideStatus.REQUESTED);
        } catch (Exception e) {
            log.warn("Failed to notify nearby drivers: {}", e.getMessage());
        }

        return mapToRideResponse(savedRide);
    }

    @Override
    public RideResponse acceptRide(UUID rideId, String driverEmail) {
        log.info("Driver {} attempting to accept ride {}", driverEmail, rideId);

        User driverUser = userRepository.findByEmail(driverEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Driver user not found with email: " + driverEmail));

        Driver driver = driverRepository.findByUser(driverUser)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found for email: " + driverEmail));

        if (!Boolean.TRUE.equals(driver.isVerified())) {
            throw new BadRequestException("Driver account is not verified yet");
        }

        if (!Boolean.TRUE.equals(driver.isAvailable())) {
            throw new BadRequestException("Driver is currently marked as unavailable");
        }

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with ID: " + rideId));

        if (ride.getStatus() != RideStatus.REQUESTED) {
            throw new RideException("Ride cannot be accepted. Current status: " + ride.getStatus());
        }

        ride.setDriver(driver);
        ride.setStatus(RideStatus.ACCEPTED);
        ride.setUpdatedAt(LocalDateTime.now());

        driver.setAvailable(false);
        driver.setUpdatedAt(LocalDateTime.now());
        driverRepository.save(driver);

        Ride savedRide = rideRepository.save(ride);
        log.info("Ride {} accepted by driver {}", rideId, driver.getId());

        notificationService.sendRideStatusUpdate(ride.getId(), RideStatus.ACCEPTED);

        return mapToRideResponse(savedRide);
    }

    @Override
    public RideResponse driverArrived(UUID rideId, String driverEmail) {
        log.info("Driver {} reporting arrival for ride {}", driverEmail, rideId);

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with ID: " + rideId));

        validateAssignedDriver(ride, driverEmail);

        if (ride.getStatus() != RideStatus.ACCEPTED) {
            throw new RideException("Driver arrival can only be marked when ride is in ACCEPTED status. Current: " + ride.getStatus());
        }

        ride.setStatus(RideStatus.DRIVER_ARRIVED);
        ride.setUpdatedAt(LocalDateTime.now());
        Ride savedRide = rideRepository.save(ride);

        notificationService.sendRideStatusUpdate(ride.getId(), RideStatus.DRIVER_ARRIVED);
        log.info("Ride {} status updated to DRIVER_ARRIVED", rideId);

        return mapToRideResponse(savedRide);
    }

    @Override
    public RideResponse startRide(UUID rideId, String otp, String driverEmail) {
        log.info("Driver {} attempting to start ride {}", driverEmail, rideId);

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with ID: " + rideId));

        validateAssignedDriver(ride, driverEmail);

        if (ride.getStatus() != RideStatus.ACCEPTED && ride.getStatus() != RideStatus.DRIVER_ARRIVED) {
            throw new RideException("Ride cannot be started. Current status: " + ride.getStatus());
        }

        if (otp == null || !otp.trim().equals(ride.getOtp())) {
            log.warn("Invalid OTP supplied for ride {}: expected {}, received {}", rideId, ride.getOtp(), otp);
            throw new BadRequestException("Invalid OTP provided. Please verify with the rider.");
        }

        ride.setStatus(RideStatus.STARTED);
        ride.setStartTime(LocalDateTime.now());
        ride.setUpdatedAt(LocalDateTime.now());
        Ride savedRide = rideRepository.save(ride);

        notificationService.sendRideStatusUpdate(ride.getId(), RideStatus.STARTED);
        log.info("Ride {} successfully started at {}", rideId, ride.getStartTime());

        return mapToRideResponse(savedRide);
    }

    @Override
    public RideResponse completeRide(UUID rideId, String driverEmail) {
        log.info("Driver {} completing ride {}", driverEmail, rideId);

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with ID: " + rideId));

        validateAssignedDriver(ride, driverEmail);

        if (ride.getStatus() != RideStatus.STARTED) {
            throw new RideException("Ride can only be completed from STARTED status. Current: " + ride.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        ride.setStatus(RideStatus.COMPLETED);
        ride.setEndTime(now);
        ride.setUpdatedAt(now);

        Driver driver = ride.getDriver();
        if (driver != null) {
            driver.setAvailable(true);
            int ridesCount = driver.getTotalRides() != null ? driver.getTotalRides() : 0;
            driver.setTotalRides(ridesCount + 1);
            driver.setUpdatedAt(now);
            driverRepository.save(driver);
        }

        Ride savedRide = rideRepository.save(ride);
        log.info("Ride {} completed successfully. Total rides for driver: {}", rideId, driver != null ? driver.getTotalRides() : 1);

        notificationService.sendRideStatusUpdate(ride.getId(), RideStatus.COMPLETED);

        return mapToRideResponse(savedRide);
    }

    @Override
    public RideResponse cancelRide(UUID rideId, String userEmail) {
        log.info("User {} requesting cancellation for ride {}", userEmail, rideId);

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with ID: " + rideId));

        // Validate user authorization: user must be rider or assigned driver
        boolean isRider = ride.getRider() != null && userEmail.equalsIgnoreCase(ride.getRider().getEmail());
        boolean isDriver = ride.getDriver() != null && ride.getDriver().getUser() != null
                && userEmail.equalsIgnoreCase(ride.getDriver().getUser().getEmail());

        if (!isRider && !isDriver) {
            throw new BadRequestException("You are not authorized to cancel this ride");
        }

        if (ride.getStatus() != RideStatus.REQUESTED
                && ride.getStatus() != RideStatus.ACCEPTED
                && ride.getStatus() != RideStatus.DRIVER_ARRIVED) {
            throw new RideException("Cannot cancel ride with current status: " + ride.getStatus());
        }

        ride.setStatus(RideStatus.CANCELLED);
        ride.setUpdatedAt(LocalDateTime.now());

        if (ride.getDriver() != null) {
            Driver driver = ride.getDriver();
            driver.setAvailable(true);
            driver.setUpdatedAt(LocalDateTime.now());
            driverRepository.save(driver);
        }

        Ride savedRide = rideRepository.save(ride);
        log.info("Ride {} has been cancelled by user {}", rideId, userEmail);

        notificationService.sendRideStatusUpdate(ride.getId(), RideStatus.CANCELLED);

        return mapToRideResponse(savedRide);
    }

    @Override
    @Transactional(readOnly = true)
    public RideResponse getRideById(UUID rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with ID: " + rideId));
        return mapToRideResponse(ride);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RideResponse> getRiderHistory(String riderEmail) {
        User rider = userRepository.findByEmail(riderEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Rider not found with email: " + riderEmail));

        return rideRepository.findByRiderOrderByCreatedAtDesc(rider).stream()
                .map(this::mapToRideResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RideResponse> getDriverHistory(String driverEmail) {
        User driverUser = userRepository.findByEmail(driverEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Driver user not found with email: " + driverEmail));

        Driver driver = driverRepository.findByUser(driverUser)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found for email: " + driverEmail));

        return rideRepository.findByDriverOrderByCreatedAtDesc(driver).stream()
                .map(this::mapToRideResponse)
                .collect(Collectors.toList());
    }

    @Override
    public FareEstimate estimateFare(double pickupLat, double pickupLng, double dropLat, double dropLng, VehicleType vehicleType) {
        double distanceInKm;
        if (geoLocationService != null) {
            try {
                distanceInKm = geoLocationService.calculateDistance(pickupLat, pickupLng, dropLat, dropLng);
            } catch (Exception e) {
                distanceInKm = calculateHaversineDistance(pickupLat, pickupLng, dropLat, dropLng);
            }
        } else {
            distanceInKm = calculateHaversineDistance(pickupLat, pickupLng, dropLat, dropLng);
        }

        VehicleType type = vehicleType != null ? vehicleType : VehicleType.SEDAN;

        double baseFare;
        double perKmRate;

        switch (type) {
            case BIKE:
                baseFare = 20.0;
                perKmRate = 7.0;
                break;
            case AUTO:
                baseFare = 30.0;
                perKmRate = 10.0;
                break;
            case HATCHBACK:
                baseFare = 40.0;
                perKmRate = 12.0;
                break;
            case SEDAN:
                baseFare = 50.0;
                perKmRate = 15.0;
                break;
            case SUV:
                baseFare = 70.0;
                perKmRate = 20.0;
                break;
            default:
                baseFare = 50.0;
                perKmRate = 15.0;
                break;
        }

        double totalFare = baseFare + (distanceInKm * perKmRate);
        BigDecimal estimatedFare = BigDecimal.valueOf(totalFare).setScale(2, RoundingMode.HALF_UP);

        int estimatedTimeMinutes;
        if (geoLocationService != null) {
            try {
                estimatedTimeMinutes = geoLocationService.calculateEta(pickupLat, pickupLng, dropLat, dropLng);
            } catch (Exception e) {
                estimatedTimeMinutes = Math.max(5, (int) Math.round((distanceInKm / 30.0) * 60));
            }
        } else {
            // Assume 30 km/h average speed in city traffic
            estimatedTimeMinutes = Math.max(5, (int) Math.round((distanceInKm / 30.0) * 60));
        }

        FareEstimate estimate = new FareEstimate();
        estimate.setVehicleType(type);
        estimate.setBaseFare(BigDecimal.valueOf(baseFare).setScale(2, RoundingMode.HALF_UP));
        estimate.setPerKmRate(BigDecimal.valueOf(perKmRate).setScale(2, RoundingMode.HALF_UP));
        estimate.setDistanceInKm(distanceInKm);
        estimate.setEstimatedFare(estimatedFare);
        estimate.setEstimatedTimeMinutes(estimatedTimeMinutes);

        return estimate;
    }

    /**
     * Calculate straight-line distance in kilometers using the Haversine formula.
     */
    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = EARTH_RADIUS_KM * c;

        return Math.round(distance * 100.0) / 100.0;
    }

    /**
     * Generate a secure random 4-digit numeric OTP.
     */
    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otpNumber = random.nextInt(9000) + 1000;
        return String.valueOf(otpNumber);
    }

    /**
     * Map Ride entity to RideResponse DTO.
     */
    private RideResponse mapToRideResponse(Ride ride) {
        if (ride == null) {
            return null;
        }

        RideResponse response = new RideResponse();
        response.setId(ride.getId());

        if (ride.getRider() != null) {
            response.setRiderId(ride.getRider().getId());
            response.setRiderName(ride.getRider().getFullName());
            response.setRiderEmail(ride.getRider().getEmail());
            response.setRiderPhone(ride.getRider().getPhoneNumber());
        }

        if (ride.getDriver() != null) {
            Driver driver = ride.getDriver();
            response.setDriverId(driver.getId());
            response.setVehicleType(driver.getVehicleType());
            response.setVehicleNumber(driver.getVehicleNumber());
            response.setVehicleModel(driver.getVehicleModel());

            if (driver.getUser() != null) {
                response.setDriverName(driver.getUser().getFullName());
                response.setDriverPhone(driver.getUser().getPhoneNumber());
            }
        }

        response.setPickupLatitude(ride.getPickupLatitude());
        response.setPickupLongitude(ride.getPickupLongitude());
        response.setDropLatitude(ride.getDropLatitude());
        response.setDropLongitude(ride.getDropLongitude());
        response.setPickupAddress(ride.getPickupAddress());
        response.setDropAddress(ride.getDropAddress());
        response.setStatus(ride.getStatus());
        response.setFare(ride.getFare());
        response.setDistanceInKm(ride.getDistanceInKm());
        response.setEstimatedTimeMinutes(ride.getEstimatedTimeMinutes());
        response.setOtp(ride.getOtp());
        response.setStartTime(ride.getStartTime());
        response.setEndTime(ride.getEndTime());
        response.setCreatedAt(ride.getCreatedAt());
        response.setUpdatedAt(ride.getUpdatedAt());

        return response;
    }

    private void validateAssignedDriver(Ride ride, String driverEmail) {
        if (ride.getDriver() == null || ride.getDriver().getUser() == null
                || !driverEmail.equalsIgnoreCase(ride.getDriver().getUser().getEmail())) {
            throw new BadRequestException("You are not the assigned driver for this ride");
        }
    }
}
