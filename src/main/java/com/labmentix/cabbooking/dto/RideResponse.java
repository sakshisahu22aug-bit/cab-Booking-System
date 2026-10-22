package com.labmentix.cabbooking.dto;

import com.labmentix.cabbooking.entity.Ride;
import com.labmentix.cabbooking.enums.RideStatus;
import com.labmentix.cabbooking.enums.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Detailed ride response mapping all ride, rider, and driver properties.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RideResponse {

    private UUID id;
    private UUID riderId;
    private String riderName;
    private String riderPhoneNumber;
    private UUID driverId;
    private String driverName;
    private String driverPhoneNumber;
    private String vehicleNumber;
    private String vehicleModel;
    private VehicleType vehicleType;
    private Double pickupLatitude;
    private Double pickupLongitude;
    private Double dropLatitude;
    private Double dropLongitude;
    private String pickupAddress;
    private String dropAddress;
    private RideStatus status;
    private BigDecimal fare;
    private Double distanceInKm;
    private Integer estimatedTimeMinutes;
    private String otp;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static RideResponse fromEntity(Ride ride) {
        if (ride == null) {
            return null;
        }

        RideResponseBuilder builder = RideResponse.builder()
                .id(ride.getId())
                .pickupLatitude(ride.getPickupLatitude())
                .pickupLongitude(ride.getPickupLongitude())
                .dropLatitude(ride.getDropLatitude())
                .dropLongitude(ride.getDropLongitude())
                .pickupAddress(ride.getPickupAddress())
                .dropAddress(ride.getDropAddress())
                .status(ride.getStatus())
                .fare(ride.getFare())
                .distanceInKm(ride.getDistanceInKm())
                .estimatedTimeMinutes(ride.getEstimatedTimeMinutes())
                .otp(ride.getOtp())
                .startTime(ride.getStartTime())
                .endTime(ride.getEndTime())
                .createdAt(ride.getCreatedAt())
                .updatedAt(ride.getUpdatedAt());

        if (ride.getRider() != null) {
            builder.riderId(ride.getRider().getId())
                   .riderName(ride.getRider().getFullName())
                   .riderPhoneNumber(ride.getRider().getPhoneNumber());
        }

        if (ride.getDriver() != null) {
            builder.driverId(ride.getDriver().getId())
                   .vehicleNumber(ride.getDriver().getVehicleNumber())
                   .vehicleModel(ride.getDriver().getVehicleModel())
                   .vehicleType(ride.getDriver().getVehicleType());

            if (ride.getDriver().getUser() != null) {
                builder.driverName(ride.getDriver().getUser().getFullName())
                       .driverPhoneNumber(ride.getDriver().getUser().getPhoneNumber());
            }
        }

        return builder.build();
    }
}
