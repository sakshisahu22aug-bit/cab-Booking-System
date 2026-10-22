package com.labmentix.cabbooking.controller;

import com.labmentix.cabbooking.dto.ApiResponse;
import com.labmentix.cabbooking.dto.PaymentRequest;
import com.labmentix.cabbooking.dto.PaymentResponse;
import com.labmentix.cabbooking.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Controller exposing payment processing and confirmation REST endpoints.
 */
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Endpoints for initiating and confirming ride payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/process")
    @Operation(summary = "Initiate payment for a ride (Card, UPI, Wallet, Cash)")
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @Valid @RequestBody PaymentRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        PaymentResponse response = paymentService.processPayment(request, userDetails.getUsername());
        return new ResponseEntity<>(ApiResponse.success("Payment initiated successfully", response), HttpStatus.CREATED);
    }

    @PostMapping("/{paymentId}/confirm")
    @Operation(summary = "Confirm payment completion")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmPayment(@PathVariable UUID paymentId) {
        PaymentResponse response = paymentService.confirmPayment(paymentId);
        return ResponseEntity.ok(ApiResponse.success("Payment marked as completed", response));
    }

    @GetMapping("/ride/{rideId}")
    @Operation(summary = "Get payment transaction details for a ride")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentByRideId(@PathVariable UUID rideId) {
        PaymentResponse response = paymentService.getPaymentByRideId(rideId);
        return ResponseEntity.ok(ApiResponse.success("Payment details retrieved", response));
    }
}
