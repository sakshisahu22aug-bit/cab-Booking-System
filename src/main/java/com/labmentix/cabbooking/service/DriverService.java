package com.labmentix.cabbooking.service;

import com.labmentix.cabbooking.dto.DriverLocationUpdate;
import com.labmentix.cabbooking.dto.DriverRegistrationRequest;
import com.labmentix.cabbooking.entity.Driver;
import com.labmentix.cabbooking.entity.User;
import com.labmentix.cabbooking.enums.UserRole;
import com.labmentix.cabbooking.enums.VehicleType;
import com.labmentix.cabbooking.exception.BadRequestException;
import com.labmentix.cabbooking.exception.ResourceNotFoundException;
import com.labmentix.cabbooking.repository.DriverRepository;
import com.labmentix.cabbooking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service managing driver onboarding, location updates, and geospatial discovery.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriverService {

    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public Driver registerDriver(DriverRegistrationRequest request, String userEmail) {
        log.info("Registering driver profile for user: {}", userEmail);

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userEmail));

        if (user.getRole() != UserRole.DRIVER) {
            user.setRole(UserRole.DRIVER);
            userRepository.save(user);
        }

        if (driverRepository.findByUser(user).isPresent()) {
            throw new BadRequestException("Driver profile already registered for this user");
        }

        if (driverRepository.existsByLicenseNumber(request.getLicenseNumber())) {
            throw new BadRequestException("License number already registered in the system");
        }

        if (driverRepository.existsByVehicleNumber(request.getVehicleNumber())) {
            throw new BadRequestException("Vehicle number already registered in the system");
        }

        Driver driver = Driver.builder()
                .user(user)
                .licenseNumber(request.getLicenseNumber())
                .vehicleType(request.getVehicleType())
                .vehicleNumber(request.getVehicleNumber())
                .vehicleModel(request.getVehicleModel())
                .available(true)
                .verified(true)
                .averageRating(5.0)
                .totalRides(0)
                .build();

        return driverRepository.save(driver);
    }

    @Transactional
    public void updateLocation(DriverLocationUpdate update) {
        Driver driver = driverRepository.findById(update.getDriverId())
                .orElseThrow(() -> new ResourceNotFoundException("Driver", "id", update.getDriverId()));

        driver.setCurrentLatitude(update.getLatitude());
        driver.setCurrentLongitude(update.getLongitude());
        driverRepository.save(driver);

        // Broadcast driver GPS coordinates over WebSocket
        messagingTemplate.convertAndSend("/topic/driver/" + driver.getId() + "/location", update);
        log.debug("Broadcasted location update for driver: {}", driver.getId());
    }

    @Transactional
    public Driver toggleAvailability(UUID driverId, boolean available) {
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", "id", driverId));

        driver.setAvailable(available);
        return driverRepository.save(driver);
    }

    @Transactional(readOnly = true)
    public List<Driver> getNearbyDrivers(Double latitude, Double longitude, Double radiusInKm, VehicleType vehicleType) {
        if (vehicleType != null) {
            return driverRepository.findNearbyDriversByVehicleType(latitude, longitude, radiusInKm, vehicleType.name());
        }
        return driverRepository.findNearbyDrivers(latitude, longitude, radiusInKm);
    }

    @Transactional(readOnly = true)
    public Driver getDriverById(UUID driverId) {
        return driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver", "id", driverId));
    }

    @Transactional(readOnly = true)
    public Driver getDriverByUserId(UUID userId) {
        return driverRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found for user: " + userId));
    }
}
