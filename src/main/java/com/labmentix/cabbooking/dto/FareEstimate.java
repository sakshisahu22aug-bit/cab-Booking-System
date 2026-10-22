package com.labmentix.cabbooking.dto;

import com.labmentix.cabbooking.enums.VehicleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO providing fare, duration, and distance estimation for a ride.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FareEstimate {

    private Double distanceInKm;
    private Integer estimatedTimeMinutes;
    private BigDecimal estimatedFare;
    private VehicleType vehicleType;
}
