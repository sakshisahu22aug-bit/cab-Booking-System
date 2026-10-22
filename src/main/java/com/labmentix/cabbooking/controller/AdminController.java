package com.labmentix.cabbooking.controller;

import com.labmentix.cabbooking.dto.ApiResponse;
import com.labmentix.cabbooking.entity.Driver;
import com.labmentix.cabbooking.entity.Payment;
import com.labmentix.cabbooking.entity.Ride;
import com.labmentix.cabbooking.entity.User;
import com.labmentix.cabbooking.enums.PaymentStatus;
import com.labmentix.cabbooking.enums.RideStatus;
import com.labmentix.cabbooking.repository.PaymentRepository;
import com.labmentix.cabbooking.repository.RideRepository;
import com.labmentix.cabbooking.repository.UserRepository;
import com.labmentix.cabbooking.service.DriverService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * REST controller providing administrative management for users, rides, unverified drivers, and dashboard metrics.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final RideRepository rideRepository;
    private final PaymentRepository paymentRepository;
    private final DriverService driverService;

    @Autowired
    public AdminController(UserRepository userRepository,
                           RideRepository rideRepository,
                           PaymentRepository paymentRepository,
                           DriverService driverService) {
        this.userRepository = userRepository;
        this.rideRepository = rideRepository;
        this.paymentRepository = paymentRepository;
        this.driverService = driverService;
    }

    /**
     * Retrieve a list of all registered platform users (ADMIN role only).
     */
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        List<User> users = userRepository.findAll();
        return ResponseEntity.ok(buildResponse(true, "All users retrieved successfully", users));
    }

    /**
     * Retrieve all rides with an optional status filter (ADMIN role only).
     */
    @GetMapping("/rides")
    public ResponseEntity<ApiResponse<List<Ride>>> getAllRides(
            @RequestParam(required = false) RideStatus status) {
        List<Ride> rides;
        if (status != null) {
            rides = rideRepository.findByStatus(status);
        } else {
            rides = rideRepository.findAll();
        }
        return ResponseEntity.ok(buildResponse(true, "Rides retrieved successfully", rides));
    }

    /**
     * Retrieve all unverified driver registrations awaiting review (ADMIN role only).
     */
    @GetMapping("/drivers/unverified")
    public ResponseEntity<ApiResponse<List<Driver>>> getUnverifiedDrivers() {
        List<Driver> unverifiedDrivers = driverService.getUnverifiedDrivers();
        return ResponseEntity.ok(buildResponse(true, "Unverified drivers retrieved successfully", unverifiedDrivers));
    }

    /**
     * Retrieve key operational dashboard statistics (ADMIN role only).
     */
    @GetMapping("/dashboard/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStats() {
        long totalUsers = userRepository.count();
        long totalRides = rideRepository.count();
        long activeDrivers = driverService.countActiveDrivers();

        BigDecimal totalRevenue;
        try {
            totalRevenue = paymentRepository.findByStatus(PaymentStatus.COMPLETED).stream()
                    .map(Payment::getAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        } catch (Exception e) {
            totalRevenue = BigDecimal.ZERO;
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("totalRides", totalRides);
        stats.put("activeDrivers", activeDrivers);
        stats.put("totalRevenue", totalRevenue);
        stats.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(buildResponse(true, "Dashboard statistics retrieved successfully", stats));
    }

    /**
     * Approve and verify a driver's credentials (ADMIN role only).
     */
    @PutMapping("/drivers/{driverId}/verify")
    public ResponseEntity<ApiResponse<Driver>> verifyDriver(@PathVariable UUID driverId) {
        Driver driver = driverService.verifyDriver(driverId);
        return ResponseEntity.ok(buildResponse(true, "Driver verified successfully", driver));
    }

    private <T> ApiResponse<T> buildResponse(boolean success, String message, T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setSuccess(success);
        response.setMessage(message);
        response.setData(data);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
}
