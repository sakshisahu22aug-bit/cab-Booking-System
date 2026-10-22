package com.labmentix.cabbooking.util;

import com.labmentix.cabbooking.entity.User;
import com.labmentix.cabbooking.exception.UnauthorizedException;
import com.labmentix.cabbooking.security.CustomUserDetails;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;
import java.util.UUID;

/**
 * Utility class providing static access to current authenticated user details from SecurityContext.
 */
public final class SecurityUtils {

    private SecurityUtils() {
        // Private constructor for utility class
    }

    /**
     * Retrieves the current authenticated user's email address from SecurityContext.
     *
     * @return Optional containing email if authenticated, empty otherwise
     */
    public static Optional<String> getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return Optional.ofNullable(userDetails.getUsername());
        } else if (principal instanceof String principalString) {
            return Optional.of(principalString);
        }
        return Optional.empty();
    }

    /**
     * Retrieves the current authenticated user's email or throws UnauthorizedException.
     *
     * @return user email
     * @throws UnauthorizedException if no authenticated user is present
     */
    public static String getCurrentUserEmailOrThrow() {
        return getCurrentUserEmail()
                .orElseThrow(() -> new UnauthorizedException("User is not authenticated"));
    }

    /**
     * Retrieves the current authenticated user's UUID from SecurityContext.
     *
     * @return Optional containing UUID if authenticated, empty otherwise
     */
    public static Optional<UUID> getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails customUserDetails) {
            return Optional.ofNullable(customUserDetails.getId());
        } else if (principal instanceof User user) {
            return Optional.ofNullable(user.getId());
        }
        return Optional.empty();
    }

    /**
     * Retrieves the current authenticated user's UUID or throws UnauthorizedException.
     *
     * @return user UUID
     * @throws UnauthorizedException if no authenticated user is present
     */
    public static UUID getCurrentUserIdOrThrow() {
        return getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedException("User is not authenticated"));
    }

    /**
     * Retrieves current UserDetails from SecurityContext.
     *
     * @return Optional containing UserDetails
     */
    public static Optional<UserDetails> getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return Optional.of(userDetails);
        }
        return Optional.empty();
    }

    /**
     * Verifies if the current session has an authenticated non-anonymous user.
     *
     * @return true if authenticated, false otherwise
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null &&
                authentication.isAuthenticated() &&
                !(authentication instanceof AnonymousAuthenticationToken);
    }
}
