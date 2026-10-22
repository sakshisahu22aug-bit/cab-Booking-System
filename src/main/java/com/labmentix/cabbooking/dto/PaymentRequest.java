package com.labmentix.cabbooking.dto;

import com.labmentix.cabbooking.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Payload for initiating ride payment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @NotNull(message = "Ride ID is required")
    private UUID rideId;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private String stripePaymentMethodId;
}
