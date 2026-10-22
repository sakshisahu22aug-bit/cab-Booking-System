package com.labmentix.cabbooking.dto;

import com.labmentix.cabbooking.enums.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for driver registration and vehicle onboarding.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverRegistrationRequest {

    @NotBlank(message = "License number cannot be blank")
    private String licenseNumber;

    @NotNull(message = "Vehicle type is required (SEDAN, SUV, HATCHBACK, AUTO, BIKE)")
    private VehicleType vehicleType;

    @NotBlank(message = "Vehicle number cannot be blank")
    @Pattern(regexp = "^[A-Z0-9 -]{5,20}$", message = "Vehicle number must be valid alphanumeric format (5-20 characters)")
    private String vehicleNumber;

    @NotBlank(message = "Vehicle model cannot be blank")
    private String vehicleModel;

    private Double currentLatitude;

    private Double currentLongitude;
}
