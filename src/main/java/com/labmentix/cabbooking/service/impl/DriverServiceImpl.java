package com.labmentix.cabbooking.service.impl;

import com.labmentix.cabbooking.dto.DriverLocationUpdate;
import com.labmentix.cabbooking.entity.Driver;
import com.labmentix.cabbooking.entity.User;
import com.labmentix.cabbooking.exception.ResourceNotFoundException;
import com.labmentix.cabbooking.repository.DriverRepository;
import com.labmentix.cabbooking.repository.UserRepository;
import com.labmentix.cabbooking.service.DriverService;
import com.labmentix.cabbooking.service.NotificationService;
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
 * Implementation of DriverService for driver location updates, availability toggling, and verification.
 */
@Service
@Transactional
public class DriverServiceImpl implements DriverService {

    private static final Logger log = LoggerFactory.getLogger(DriverServiceImpl.class);
    private static final double EARTH_RADIUS_KM = 6371.0;

    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Autowired
    public DriverServiceImpl(DriverRepository driverRepository,
                             UserRepository userRepository,
                             NotificationService notificationService) {
        this.driverRepository = driverRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Override
    public Driver updateLocation(String driverEmail, DriverLocationUpdate update) {
        log.info("Updating location for driver: {}", driverEmail);

        User user = userRepository.findByEmail(driverEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + driverEmail));

        Driver driver = driverRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found for email: " + driverEmail));

        driver.setCurrentLatitude(update.getLatitude());
        driver.setCurrentLongitude(update.getLongitude());
        driver.setUpdatedAt(LocalDateTime.now());

        Driver savedDriver = driverRepository.save(driver);

        if (update.getDriverId() == null) {
            update.setDriverId(savedDriver.getId());
        }

        try {
            notificationService.sendDriverLocationUpdate(update);
        } catch (Exception e) {
            log.warn("Failed to broadcast driver location update: {}", e.getMessage());
        }

        return savedDriver;
    }

    @Override
    public boolean toggleAvailability(String driverEmail, Boolean available) {
        log.info("Toggling availability for driver: {}", driverEmail);

        User user = userRepository.findByEmail(driverEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + driverEmail));

        Driver driver = driverRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found for email: " + driverEmail));

        boolean newStatus = (available != null) ? available : !Boolean.TRUE.equals(driver.isAvailable());
        driver.setAvailable(newStatus);
        driver.setUpdatedAt(LocalDateTime.now());

        driverRepository.save(driver);
        log.info("Driver {} availability set to {}", driver.getId(), newStatus);

        return newStatus;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Driver> findNearbyDrivers(double lat, double lng, double radius) {
        double maxRadius = (radius > 0) ? radius : 5.0;

        List<Driver> activeDrivers = driverRepository.findByAvailableTrueAndVerifiedTrue();

        return activeDrivers.stream()
                .filter(d -> d.getCurrentLatitude() != null && d.getCurrentLongitude() != null)
                .filter(d -> calculateDistance(lat, lng, d.getCurrentLatitude(), d.getCurrentLongitude()) <= maxRadius)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Driver getDriverById(UUID driverId) {
        return driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with ID: " + driverId));
    }

    @Override
    @Transactional(readOnly = true)
    public Driver getDriverByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

        return driverRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Driver profile not found for email: " + email));
    }

    @Override
    public Driver verifyDriver(UUID driverId) {
        log.info("Admin verifying driver: {}", driverId);

        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new ResourceNotFoundException("Driver not found with ID: " + driverId));

        driver.setVerified(true);
        driver.setUpdatedAt(LocalDateTime.now());

        Driver verifiedDriver = driverRepository.save(driver);
        log.info("Driver {} successfully verified", driverId);

        return verifiedDriver;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Driver> getUnverifiedDrivers() {
        try {
            return driverRepository.findByVerifiedFalse();
        } catch (Exception e) {
            // Fallback in case repository uses stream/filter
            return driverRepository.findAll().stream()
                    .filter(d -> !Boolean.TRUE.equals(d.isVerified()))
                    .collect(Collectors.toList());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveDrivers() {
        try {
            return driverRepository.countByAvailableTrue();
        } catch (Exception e) {
            return driverRepository.findAll().stream()
                    .filter(d -> Boolean.TRUE.equals(d.isAvailable()))
                    .count();
        }
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
