package com.labmentix.cabbooking.enums;

import java.math.BigDecimal;

/**
 * Types of vehicles supported by the Cab Booking System.
 */
public enum VehicleType {
    SEDAN(new BigDecimal("50.00"), new BigDecimal("15.00"), 4),
    SUV(new BigDecimal("80.00"), new BigDecimal("20.00"), 6),
    HATCHBACK(new BigDecimal("40.00"), new BigDecimal("12.00"), 4),
    AUTO(new BigDecimal("30.00"), new BigDecimal("10.00"), 3),
    BIKE(new BigDecimal("20.00"), new BigDecimal("7.00"), 1);

    private final BigDecimal baseFare;
    private final BigDecimal perKmRate;
    private final int capacity;

    VehicleType(BigDecimal baseFare, BigDecimal perKmRate, int capacity) {
        this.baseFare = baseFare;
        this.perKmRate = perKmRate;
        this.capacity = capacity;
    }

    public BigDecimal getBaseFare() {
        return baseFare;
    }

    public BigDecimal getPerKmRate() {
        return perKmRate;
    }

    public int getCapacity() {
        return capacity;
    }
}
