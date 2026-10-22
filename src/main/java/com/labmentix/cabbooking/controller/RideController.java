package com.labmentix.cabbooking.controller;

import com.labmentix.cabbooking.dto.ApiResponse;
import com.labmentix.cabbooking.dto.FareEstimate;
import com.labmentix.cabbooking.dto.RideRequest;
import com.labmentix.cabbooking.dto.RideResponse;
import com.labmentix.cabbooking.enums.VehicleType;
import com.labmentix.cabbooking.security.SecurityUtils;
import com.labmentix.cabbooking.service.RideService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for managing ride bookings, status changes, fare estimates, and ride history.
 */
@RestController
@RequestMapping("/api/rides")
public class RideController {

    private final RideService rideService;

    @Autowired
    public RideController(RideService rideService) {
        this.rideService = rideService;
    }

    /**
     * Request a new ride booking (RIDER role only).
     */
    @PostMapping
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<ApiResponse<RideResponse>> requestRide(@Valid @RequestBody RideRequest request,
                                                                 Principal principal) {
        String riderEmail = resolveUserEmail(principal);
        RideResponse rideResponse = rideService.requestRide(request, riderEmail);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(buildResponse(true, "Ride requested successfully", rideResponse));
    }

    /**
     * Driver accepts an available ride (DRIVER role only).
     */
    @PostMapping("/{rideId}/accept")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<RideResponse>> acceptRide(@PathVariable UUID rideId,
                                                                Principal principal) {
        String driverEmail = resolveUserEmail(principal);
        RideResponse rideResponse = rideService.acceptRide(rideId, driverEmail);
        return ResponseEntity.ok(buildResponse(true, "Ride accepted successfully", rideResponse));
    }

    /**
     * Driver marks arrival at the pickup location (DRIVER role only).
     */
    @PostMapping("/{rideId}/arrived")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<RideResponse>> driverArrived(@PathVariable UUID rideId,
                                                                  Principal principal) {
        String driverEmail = resolveUserEmail(principal);
        RideResponse rideResponse = rideService.driverArrived(rideId, driverEmail);
        return ResponseEntity.ok(buildResponse(true, "Driver marked as arrived at pickup location", rideResponse));
    }

    /**
     * Driver starts ride after verifying OTP from rider (DRIVER role only).
     */
    @PostMapping("/{rideId}/start")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<RideResponse>> startRide(@PathVariable UUID rideId,
                                                              @RequestParam(required = false) String otp,
                                                              @RequestBody(required = false) Map<String, String> body,
                                                              Principal principal) {
        String driverEmail = resolveUserEmail(principal);
        String finalOtp = otp != null ? otp : (body != null ? body.get("otp") : null);
        RideResponse rideResponse = rideService.startRide(rideId, finalOtp, driverEmail);
        return ResponseEntity.ok(buildResponse(true, "Ride started successfully", rideResponse));
    }

    /**
     * Driver completes an ongoing ride (DRIVER role only).
     */
    @PostMapping("/{rideId}/complete")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<RideResponse>> completeRide(@PathVariable UUID rideId,
                                                                 Principal principal) {
        String driverEmail = resolveUserEmail(principal);
        RideResponse rideResponse = rideService.completeRide(rideId, driverEmail);
        return ResponseEntity.ok(buildResponse(true, "Ride completed successfully", rideResponse));
    }

    /**
     * Cancel an existing ride (RIDER or DRIVER role).
     */
    @PostMapping("/{rideId}/cancel")
    @PreAuthorize("hasAnyRole('RIDER', 'DRIVER')")
    public ResponseEntity<ApiResponse<RideResponse>> cancelRide(@PathVariable UUID rideId,
                                                               Principal principal) {
        String userEmail = resolveUserEmail(principal);
        RideResponse rideResponse = rideService.cancelRide(rideId, userEmail);
        return ResponseEntity.ok(buildResponse(true, "Ride cancelled successfully", rideResponse));
    }

    /**
     * Retrieve ride details by ID (authenticated users).
     */
    @GetMapping("/{rideId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<RideResponse>> getRideById(@PathVariable UUID rideId) {
        RideResponse rideResponse = rideService.getRideById(rideId);
        return ResponseEntity.ok(buildResponse(true, "Ride details retrieved successfully", rideResponse));
    }

    /**
     * Retrieve ride history for the authenticated rider.
     */
    @GetMapping("/history/rider")
    @PreAuthorize("hasRole('RIDER')")
    public ResponseEntity<ApiResponse<List<RideResponse>>> getRiderHistory(Principal principal) {
        String riderEmail = resolveUserEmail(principal);
        List<RideResponse> history = rideService.getRiderHistory(riderEmail);
        return ResponseEntity.ok(buildResponse(true, "Rider history retrieved successfully", history));
    }

    /**
     * Retrieve ride history for the authenticated driver.
     */
    @GetMapping("/history/driver")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<List<RideResponse>>> getDriverHistory(Principal principal) {
        String driverEmail = resolveUserEmail(principal);
        List<RideResponse> history = rideService.getDriverHistory(driverEmail);
        return ResponseEntity.ok(buildResponse(true, "Driver history retrieved successfully", history));
    }

    /**
     * Estimate fare between two coordinates for a given vehicle type (authenticated users).
     */
    @GetMapping("/estimate")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<FareEstimate>> estimateFare(
            @RequestParam double pickupLat,
            @RequestParam double pickupLng,
            @RequestParam double dropLat,
            @RequestParam double dropLng,
            @RequestParam(defaultValue = "SEDAN") VehicleType vehicleType) {
        FareEstimate estimate = rideService.estimateFare(pickupLat, pickupLng, dropLat, dropLng, vehicleType);
        return ResponseEntity.ok(buildResponse(true, "Fare estimate calculated successfully", estimate));
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
