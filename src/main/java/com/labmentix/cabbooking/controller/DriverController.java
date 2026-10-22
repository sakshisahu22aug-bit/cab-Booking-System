package com.labmentix.cabbooking.controller;

import com.labmentix.cabbooking.dto.ApiResponse;
import com.labmentix.cabbooking.dto.DriverLocationUpdate;
import com.labmentix.cabbooking.entity.Driver;
import com.labmentix.cabbooking.security.SecurityUtils;
import com.labmentix.cabbooking.service.DriverService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for driver telemetry updates, availability status, driver search, and admin verification.
 */
@RestController
@RequestMapping("/api/drivers")
public class DriverController {

    private final DriverService driverService;

    @Autowired
    public DriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    /**
     * Update current GPS location of the driver (DRIVER role only).
     */
    @PutMapping("/location")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<String>> updateLocation(@Valid @RequestBody DriverLocationUpdate request,
                                                              Principal principal) {
        String driverEmail = resolveUserEmail(principal);
        driverService.updateLocation(driverEmail, request);
        return ResponseEntity.ok(buildResponse(true, "Driver location updated successfully", "Location updated"));
    }

    /**
     * Toggle or explicitly set driver online availability (DRIVER role only).
     */
    @PutMapping("/availability")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<Boolean>> toggleAvailability(
            @RequestParam(required = false) Boolean available,
            Principal principal) {
        String driverEmail = resolveUserEmail(principal);
        boolean isAvailable = driverService.toggleAvailability(driverEmail, available);
        String message = isAvailable ? "Driver is now ONLINE and available for rides" : "Driver is now OFFLINE";
        return ResponseEntity.ok(buildResponse(true, message, isAvailable));
    }

    /**
     * Locate nearby available drivers within a search radius (authenticated users).
     */
    @GetMapping("/nearby")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Driver>>> findNearbyDrivers(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "5.0") double radius) {
        List<Driver> nearbyDrivers = driverService.findNearbyDrivers(lat, lng, radius);
        return ResponseEntity.ok(buildResponse(true, "Nearby drivers retrieved successfully", nearbyDrivers));
    }

    /**
     * Retrieve driver profile by UUID (authenticated users).
     */
    @GetMapping("/{driverId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Driver>> getDriverById(@PathVariable UUID driverId) {
        Driver driver = driverService.getDriverById(driverId);
        return ResponseEntity.ok(buildResponse(true, "Driver profile retrieved successfully", driver));
    }

    /**
     * Verify driver account and documents (ADMIN role only).
     */
    @PutMapping("/verify/{driverId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Driver>> verifyDriver(@PathVariable UUID driverId) {
        Driver driver = driverService.verifyDriver(driverId);
        return ResponseEntity.ok(buildResponse(true, "Driver verified successfully", driver));
    }

    private String resolveUserEmail(Principal principal) {
        if (principal != null && principal.getName() != null && !principal.getName().isBlank()) {
            return principal.getName();
        }
        return SecurityUtils.getCurrentUserEmail();
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
