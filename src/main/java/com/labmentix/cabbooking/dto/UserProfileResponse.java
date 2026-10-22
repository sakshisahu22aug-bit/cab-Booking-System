package com.labmentix.cabbooking.dto;

import com.labmentix.cabbooking.enums.UserRole;
import com.labmentix.cabbooking.enums.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * User profile response DTO containing user information and optional driver profile details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private UUID id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private UserRole role;
    private String profileImageUrl;
    private boolean emailVerified;
    private LocalDateTime createdAt;
    private DriverDetailsDto driverDetails;

    /**
     * Nested DTO containing driver-specific details if user has the DRIVER role.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DriverDetailsDto {
        private UUID driverId;
        private String licenseNumber;
        private VehicleType vehicleType;
        private String vehicleNumber;
        private String vehicleModel;
        private boolean available;
        private boolean verified;
        private double averageRating;
        private int totalRides;
        private Double currentLatitude;
        private Double currentLongitude;
    }
}
