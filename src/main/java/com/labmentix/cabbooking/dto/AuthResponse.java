package com.labmentix.cabbooking.dto;

import com.labmentix.cabbooking.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Authentication response containing JWT token and user profile metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;

    @Builder.Default
    private String tokenType = "Bearer";

    private UUID userId;

    private String email;

    private String fullName;

    private UserRole role;
}
