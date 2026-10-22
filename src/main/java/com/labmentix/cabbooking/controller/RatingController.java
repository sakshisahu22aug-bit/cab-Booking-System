package com.labmentix.cabbooking.controller;

import com.labmentix.cabbooking.dto.ApiResponse;
import com.labmentix.cabbooking.dto.RatingRequest;
import com.labmentix.cabbooking.dto.RatingResponse;
import com.labmentix.cabbooking.service.RatingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Controller exposing endpoints for submitting feedback and viewing rating histories.
 */
@RestController
@RequestMapping("/ratings")
@RequiredArgsConstructor
@Tag(name = "Ratings", description = "Endpoints for reviewing and rating completed rides")
public class RatingController {

    private final RatingService ratingService;

    @PostMapping
    @Operation(summary = "Submit a rating and review for a completed ride")
    public ResponseEntity<ApiResponse<RatingResponse>> submitRating(
            @Valid @RequestBody RatingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        RatingResponse response = ratingService.submitRating(request, userDetails.getUsername());
        return new ResponseEntity<>(ApiResponse.success("Rating submitted successfully", response), HttpStatus.CREATED);
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get all ratings received by a specific user or driver")
    public ResponseEntity<ApiResponse<List<RatingResponse>>> getRatingsForUser(@PathVariable UUID userId) {
        List<RatingResponse> ratings = ratingService.getRatingsForUser(userId);
        return ResponseEntity.ok(ApiResponse.success("Ratings retrieved successfully", ratings));
    }

    @GetMapping("/ride/{rideId}")
    @Operation(summary = "Get the rating given for a specific ride")
    public ResponseEntity<ApiResponse<RatingResponse>> getRatingForRide(@PathVariable UUID rideId) {
        RatingResponse response = ratingService.getRatingForRide(rideId);
        return ResponseEntity.ok(ApiResponse.success("Ride rating retrieved", response));
    }
}
