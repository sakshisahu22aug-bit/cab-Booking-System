package com.labmentix.cabbooking.controller;

import com.labmentix.cabbooking.dto.AuthResponse;
import com.labmentix.cabbooking.dto.DriverRegistrationRequest;
import com.labmentix.cabbooking.dto.LoginRequest;
import com.labmentix.cabbooking.dto.RegisterRequest;
import com.labmentix.cabbooking.dto.UserProfileResponse;
import com.labmentix.cabbooking.entity.User;
import com.labmentix.cabbooking.exception.ResourceNotFoundException;
import com.labmentix.cabbooking.repository.UserRepository;
import com.labmentix.cabbooking.service.AuthService;
import com.labmentix.cabbooking.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing endpoints for user registration, authentication, driver onboarding, and profile retrieval.
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication & User Management", description = "Endpoints for signup, signin, driver onboarding, and user profile")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    /**
     * Registers a new user (Rider, Driver, or Admin) and returns a JWT token.
     */
    @Operation(summary = "Register new user", description = "Validates user input, hashes password, saves record, and issues JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User successfully registered"),
            @ApiResponse(responseCode = "400", description = "Validation failed or duplicate email/phone")
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Received registration request for email: {}", request.getEmail());
        AuthResponse response = authService.registerUser(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Authenticates user credentials and issues a JWT token.
     */
    @Operation(summary = "Login user", description = "Authenticates credentials and returns a signed JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Received login request for email: {}", request.getEmail());
        AuthResponse response = authService.loginUser(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Onboards an authenticated user as a driver with vehicle and license details.
     */
    @Operation(summary = "Register as driver", description = "Requires authentication. Onboards driver vehicle profile.")
    @SecurityRequirement(name = "BearerAuthentication")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Driver successfully registered"),
            @ApiResponse(responseCode = "400", description = "Driver profile already exists or invalid vehicle details"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated")
    })
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/register/driver")
    public ResponseEntity<UserProfileResponse> registerDriver(
            @Valid @RequestBody DriverRegistrationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        String email = userDetails != null ? userDetails.getUsername() : SecurityUtils.getCurrentUserEmailOrThrow();
        log.info("Received driver onboarding request for authenticated user: {}", email);

        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        UserProfileResponse response = authService.registerDriver(request, currentUser);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Retrieves the profile of the currently authenticated user.
     */
    @Operation(summary = "Get current user profile", description = "Retrieves profile and role details for the authenticated user")
    @SecurityRequirement(name = "BearerAuthentication")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated")
    })
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails != null ? userDetails.getUsername() : SecurityUtils.getCurrentUserEmailOrThrow();
        log.debug("Fetching current user profile for email: {}", email);

        UserProfileResponse response = authService.getCurrentUserProfile(email);
        return ResponseEntity.ok(response);
    }
}
