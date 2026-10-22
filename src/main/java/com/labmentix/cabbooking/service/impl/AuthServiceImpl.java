package com.labmentix.cabbooking.service.impl;

import com.labmentix.cabbooking.config.security.JwtTokenProvider;
import com.labmentix.cabbooking.dto.*;
import com.labmentix.cabbooking.entity.Driver;
import com.labmentix.cabbooking.entity.User;
import com.labmentix.cabbooking.enums.UserRole;
import com.labmentix.cabbooking.exception.BadRequestException;
import com.labmentix.cabbooking.exception.ResourceNotFoundException;
import com.labmentix.cabbooking.repository.DriverRepository;
import com.labmentix.cabbooking.repository.UserRepository;
import com.labmentix.cabbooking.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Implementation of AuthService managing user authentication, registration, and driver onboarding.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public AuthResponse registerUser(RegisterRequest request) {
        log.info("Processing user registration for email: {}", request.getEmail());

        String email = request.getEmail().toLowerCase().trim();
        String phoneNumber = request.getPhoneNumber().trim();

        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("An account with email " + email + " already exists.");
        }

        if (userRepository.existsByPhoneNumber(phoneNumber)) {
            throw new BadRequestException("An account with phone number " + phoneNumber + " already exists.");
        }

        UserRole role = request.getRole() != null ? request.getRole() : UserRole.RIDER;

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(phoneNumber)
                .role(role)
                .emailVerified(false)
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with ID: {} and role: {}", savedUser.getId(), savedUser.getRole());

        String token = jwtTokenProvider.generateToken(savedUser);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(savedUser.getId())
                .email(savedUser.getEmail())
                .fullName(savedUser.getFullName())
                .role(savedUser.getRole())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse loginUser(LoginRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        log.info("Authenticating user with email: {}", email);

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword())
        );

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        String token = jwtTokenProvider.generateToken(authentication);
        log.info("User logged in successfully with ID: {}", user.getId());

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }

    @Override
    @Transactional
    public UserProfileResponse registerDriver(DriverRegistrationRequest request, User currentUser) {
        log.info("Processing driver onboarding for user ID: {}", currentUser.getId());

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", currentUser.getId()));

        if (driverRepository.findByUser(user).isPresent()) {
            throw new BadRequestException("A driver profile already exists for this user account.");
        }

        String licenseNumber = request.getLicenseNumber().toUpperCase().trim();
        if (driverRepository.existsByLicenseNumber(licenseNumber)) {
            throw new BadRequestException("License number " + licenseNumber + " is already registered with another driver.");
        }

        String vehicleNumber = request.getVehicleNumber().toUpperCase().trim();
        if (driverRepository.existsByVehicleNumber(vehicleNumber)) {
            throw new BadRequestException("Vehicle number " + vehicleNumber + " is already registered.");
        }

        // Upgrade user role to DRIVER if not already set
        if (user.getRole() != UserRole.DRIVER) {
            user.setRole(UserRole.DRIVER);
            user = userRepository.save(user);
        }

        Driver driver = Driver.builder()
                .user(user)
                .licenseNumber(licenseNumber)
                .vehicleType(request.getVehicleType())
                .vehicleNumber(vehicleNumber)
                .vehicleModel(request.getVehicleModel().trim())
                .currentLatitude(request.getCurrentLatitude())
                .currentLongitude(request.getCurrentLongitude())
                .available(true)
                .verified(false) // Pending admin verification
                .averageRating(5.0)
                .totalRides(0)
                .build();

        Driver savedDriver = driverRepository.save(driver);
        log.info("Driver profile created successfully with ID: {} for user ID: {}", savedDriver.getId(), user.getId());

        return mapToUserProfileResponse(user, savedDriver);
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentUserProfile(String email) {
        String normalizedEmail = email.toLowerCase().trim();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", normalizedEmail));

        Driver driver = null;
        if (user.getRole() == UserRole.DRIVER) {
            driver = driverRepository.findByUser(user).orElse(null);
        }

        return mapToUserProfileResponse(user, driver);
    }

    private UserProfileResponse mapToUserProfileResponse(User user, Driver driver) {
        UserProfileResponse.DriverDetailsDto driverDetails = null;

        if (driver != null) {
            driverDetails = UserProfileResponse.DriverDetailsDto.builder()
                    .driverId(driver.getId())
                    .licenseNumber(driver.getLicenseNumber())
                    .vehicleType(driver.getVehicleType())
                    .vehicleNumber(driver.getVehicleNumber())
                    .vehicleModel(driver.getVehicleModel())
                    .available(driver.isAvailable())
                    .verified(driver.isVerified())
                    .averageRating(driver.getAverageRating())
                    .totalRides(driver.getTotalRides())
                    .currentLatitude(driver.getCurrentLatitude())
                    .currentLongitude(driver.getCurrentLongitude())
                    .build();
        }

        return UserProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .profileImageUrl(user.getProfileImageUrl())
                .emailVerified(user.isEmailVerified())
                .createdAt(user.getCreatedAt())
                .driverDetails(driverDetails)
                .build();
    }
}
