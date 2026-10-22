package com.labmentix.cabbooking.dto;

import com.labmentix.cabbooking.entity.Payment;
import com.labmentix.cabbooking.enums.PaymentMethod;
import com.labmentix.cabbooking.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Detailed payment response.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private UUID paymentId;
    private UUID rideId;
    private BigDecimal amount;
    private String currency;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private String stripePaymentIntentId;
    private String stripeClientSecret;
    private LocalDateTime createdAt;

    public static PaymentResponse fromEntity(Payment payment, String stripeClientSecret) {
        if (payment == null) {
            return null;
        }

        return PaymentResponse.builder()
                .paymentId(payment.getId())
                .rideId(payment.getRide() != null ? payment.getRide().getId() : null)
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus())
                .stripePaymentIntentId(payment.getStripePaymentIntentId())
                .stripeClientSecret(stripeClientSecret)
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
