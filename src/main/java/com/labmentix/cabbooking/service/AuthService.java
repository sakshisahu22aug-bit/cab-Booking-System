package com.labmentix.cabbooking.service;

import com.labmentix.cabbooking.dto.AuthResponse;
import com.labmentix.cabbooking.dto.DriverRegistrationRequest;
import com.labmentix.cabbooking.dto.LoginRequest;
import com.labmentix.cabbooking.dto.RegisterRequest;
import com.labmentix.cabbooking.dto.UserProfileResponse;
import com.labmentix.cabbooking.entity.User;

/**
 * Service interface defining authentication, user registration, and driver onboarding business logic.
 */
public interface AuthService {

    /**
     * Registers a new user account (RIDER, DRIVER, or ADMIN), hashes password, and issues JWT.
     *
     * @param request user registration details
     * @return authentication response containing JWT and user profile
     */
    AuthResponse registerUser(RegisterRequest request);

    /**
     * Authenticates an existing user via email and password, issuing a fresh JWT.
     *
     * @param request login credentials
     * @return authentication response containing JWT and user profile
     */
    AuthResponse loginUser(LoginRequest request);

    /**
     * Onboards an authenticated user as a driver, establishing their vehicle profile.
     *
     * @param request     driver vehicle and license details
     * @param currentUser currently authenticated user
     * @return updated user profile with driver details
     */
    UserProfileResponse registerDriver(DriverRegistrationRequest request, User currentUser);

    /**
     * Retrieves the profile information for the specified user email.
     *
     * @param email user email address
     * @return detailed user profile response
     */
    UserProfileResponse getCurrentUserProfile(String email);
}
