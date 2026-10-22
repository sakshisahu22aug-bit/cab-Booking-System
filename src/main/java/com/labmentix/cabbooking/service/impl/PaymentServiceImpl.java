package com.labmentix.cabbooking.service.impl;

import com.labmentix.cabbooking.dto.PaymentRequest;
import com.labmentix.cabbooking.dto.PaymentResponse;
import com.labmentix.cabbooking.entity.*;
import com.labmentix.cabbooking.enums.*;
import com.labmentix.cabbooking.exception.BadRequestException;
import com.labmentix.cabbooking.exception.PaymentException;
import com.labmentix.cabbooking.exception.ResourceNotFoundException;
import com.labmentix.cabbooking.repository.PaymentRepository;
import com.labmentix.cabbooking.repository.RideRepository;
import com.labmentix.cabbooking.repository.UserRepository;
import com.labmentix.cabbooking.service.PaymentService;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of PaymentService providing Stripe payment processing, webhook confirmation, and refund capabilities.
 */
@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    @Value("${stripe.api.key:}")
    private String stripeApiKey;

    private final PaymentRepository paymentRepository;
    private final RideRepository rideRepository;
    private final UserRepository userRepository;

    @Autowired
    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              RideRepository rideRepository,
                              UserRepository userRepository) {
        this.paymentRepository = paymentRepository;
        this.rideRepository = rideRepository;
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void init() {
        if (stripeApiKey != null && !stripeApiKey.isBlank()) {
            Stripe.apiKey = stripeApiKey;
            log.info("Stripe API key initialized successfully.");
        } else {
            log.warn("Stripe API key is not configured. Real Stripe API calls will fail.");
        }
    }

    @Override
    public PaymentResponse createPayment(PaymentRequest request, String userEmail) {
        log.info("Initiating payment for ride: {} by user: {}", request.getRideId(), userEmail);

        User rider = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        Ride ride = rideRepository.findById(request.getRideId())
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with ID: " + request.getRideId()));

        if (ride.getRider() == null || !ride.getRider().getId().equals(rider.getId())) {
            throw new BadRequestException("You are not authorized to make payment for this ride");
        }

        if (ride.getStatus() != RideStatus.COMPLETED) {
            throw new BadRequestException("Payment can only be made for completed rides. Current status: " + ride.getStatus());
        }

        Optional<Payment> existingPayment = paymentRepository.findByRide(ride);
        if (existingPayment.isPresent()) {
            Payment payment = existingPayment.get();
            if (payment.getStatus() == PaymentStatus.COMPLETED) {
                throw new BadRequestException("Payment has already been completed for this ride");
            }
        }

        BigDecimal amount = request.getAmount() != null ? request.getAmount() : ride.getFare();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Invalid payment amount: " + amount);
        }

        String currency = request.getCurrency() != null ? request.getCurrency().toLowerCase() : "inr";
        PaymentMethod paymentMethod = request.getPaymentMethod() != null ? request.getPaymentMethod() : PaymentMethod.CARD;

        String stripePaymentIntentId = null;
        String clientSecret = null;

        if (paymentMethod == PaymentMethod.CARD) {
            try {
                long amountInSmallestUnit = amount.multiply(BigDecimal.valueOf(100)).longValue();

                PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                        .setAmount(amountInSmallestUnit)
                        .setCurrency(currency)
                        .setAutomaticPaymentMethods(
                                PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build()
                        )
                        .putMetadata("rideId", ride.getId().toString())
                        .putMetadata("riderEmail", userEmail)
                        .build();

                PaymentIntent paymentIntent = PaymentIntent.create(params);
                stripePaymentIntentId = paymentIntent.getId();
                clientSecret = paymentIntent.getClientSecret();
                log.info("Stripe PaymentIntent created: {}", stripePaymentIntentId);
            } catch (StripeException e) {
                log.error("Stripe error while creating PaymentIntent: {}", e.getMessage(), e);
                throw new PaymentException("Failed to initiate card payment with Stripe: " + e.getMessage());
            }
        }

        Payment payment = existingPayment.orElseGet(Payment::new);
        payment.setRide(ride);
        payment.setRider(rider);
        payment.setAmount(amount);
        payment.setCurrency(currency.toUpperCase());
        payment.setPaymentMethod(paymentMethod);
        payment.setStripePaymentIntentId(stripePaymentIntentId);
        payment.setStatus(PaymentStatus.PENDING);
        if (payment.getCreatedAt() == null) {
            payment.setCreatedAt(LocalDateTime.now());
        }

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment record created with ID: {} and status: PENDING", savedPayment.getId());

        return mapToPaymentResponse(savedPayment, clientSecret);
    }

    @Override
    public PaymentResponse confirmPayment(String stripePaymentIntentId) {
        log.info("Confirming payment for Stripe PaymentIntent: {}", stripePaymentIntentId);

        Payment payment = paymentRepository.findByStripePaymentIntentId(stripePaymentIntentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for Stripe Intent: " + stripePaymentIntentId));

        payment.setStatus(PaymentStatus.COMPLETED);
        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment {} successfully confirmed and marked as COMPLETED", savedPayment.getId());

        return mapToPaymentResponse(savedPayment, null);
    }

    @Override
    public PaymentResponse processRefund(UUID paymentId) {
        log.info("Processing refund for payment ID: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + paymentId));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new BadRequestException("Only COMPLETED payments can be refunded. Current status: " + payment.getStatus());
        }

        if (payment.getStripePaymentIntentId() != null && !payment.getStripePaymentIntentId().isBlank()) {
            try {
                RefundCreateParams params = RefundCreateParams.builder()
                        .setPaymentIntent(payment.getStripePaymentIntentId())
                        .build();

                Refund refund = Refund.create(params);
                log.info("Stripe refund processed with status: {}, ID: {}", refund.getStatus(), refund.getId());
            } catch (StripeException e) {
                log.error("Stripe error during refund: {}", e.getMessage(), e);
                throw new PaymentException("Failed to process refund with Stripe: " + e.getMessage());
            }
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment {} updated to REFUNDED status", paymentId);

        return mapToPaymentResponse(savedPayment, null);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getPaymentByRide(UUID rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new ResourceNotFoundException("Ride not found with ID: " + rideId));

        Payment payment = paymentRepository.findByRide(ride)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for ride ID: " + rideId));

        return mapToPaymentResponse(payment, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentHistory(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + userEmail));

        return paymentRepository.findByRiderOrderByCreatedAtDesc(user).stream()
                .map(p -> mapToPaymentResponse(p, null))
                .collect(Collectors.toList());
    }

    private PaymentResponse mapToPaymentResponse(Payment payment, String clientSecret) {
        if (payment == null) {
            return null;
        }

        PaymentResponse response = new PaymentResponse();
        response.setId(payment.getId());
        if (payment.getRide() != null) {
            response.setRideId(payment.getRide().getId());
        }
        if (payment.getRider() != null) {
            response.setRiderId(payment.getRider().getId());
        }
        response.setAmount(payment.getAmount());
        response.setCurrency(payment.getCurrency());
        response.setPaymentMethod(payment.getPaymentMethod());
        response.setStripePaymentIntentId(payment.getStripePaymentIntentId());
        response.setClientSecret(clientSecret);
        response.setStatus(payment.getStatus());
        response.setCreatedAt(payment.getCreatedAt());

        return response;
    }
}
