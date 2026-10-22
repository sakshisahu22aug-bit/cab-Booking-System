package com.labmentix.cabbooking.config.security;

import com.labmentix.cabbooking.entity.User;
import com.labmentix.cabbooking.security.CustomUserDetails;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Utility class for generating, parsing, and validating JSON Web Tokens (JWT) using JJWT 0.12.5.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${app.jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String jwtSecret;

    @Value("${app.jwt.expiration:${app.jwt.expiration-ms:86400000}}")
    private long jwtExpirationInMs;

    /**
     * Obtains the cryptographic signing key for HMAC-SHA algorithms.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(jwtSecret);
            if (keyBytes.length < 32) {
                keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
            }
        } catch (IllegalArgumentException e) {
            keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        }

        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates a signed JWT token from an authenticated UserDetails instance.
     *
     * @param userDetails authenticated user details
     * @return signed JWT token string
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();

        String role = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_RIDER");
        claims.put("role", role);

        if (userDetails instanceof CustomUserDetails customUserDetails) {
            claims.put("userId", customUserDetails.getId().toString());
            claims.put("email", customUserDetails.getEmail());
            claims.put("fullName", customUserDetails.getFullName());
        } else if (userDetails instanceof User user) {
            claims.put("userId", user.getId().toString());
            claims.put("email", user.getEmail());
            claims.put("fullName", user.getFullName());
        }

        return createToken(claims, userDetails.getUsername());
    }

    /**
     * Generates a signed JWT token from an Authentication object.
     *
     * @param authentication active authentication object
     * @return signed JWT token string
     */
    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return generateToken(userDetails);
    }

    /**
     * Generates a signed JWT token directly from user credentials.
     *
     * @param email  user email
     * @param userId user UUID
     * @param role   user role
     * @return signed JWT token string
     */
    public String generateToken(String email, UUID userId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        if (userId != null) {
            claims.put("userId", userId.toString());
        }
        claims.put("role", role.startsWith("ROLE_") ? role : "ROLE_" + role);
        return createToken(claims, email);
    }

    /**
     * Builds and signs the JWT compact token.
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Extracts username/email subject from JWT token.
     *
     * @param token JWT token string
     * @return username or email
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts email address from JWT token subject or email claim.
     *
     * @param token JWT token string
     * @return email address
     */
    public String extractEmail(String token) {
        Claims claims = extractAllClaims(token);
        String emailClaim = claims.get("email", String.class);
        return emailClaim != null ? emailClaim : claims.getSubject();
    }

    /**
     * Extracts user UUID from JWT claims.
     *
     * @param token JWT token string
     * @return UUID of user or null if not present
     */
    public UUID extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        String userIdStr = claims.get("userId", String.class);
        return userIdStr != null ? UUID.fromString(userIdStr) : null;
    }

    /**
     * Extracts authority role from JWT claims.
     *
     * @param token JWT token string
     * @return user role string
     */
    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("role", String.class);
    }

    /**
     * Extracts expiration timestamp from JWT token.
     *
     * @param token JWT token string
     * @return token expiration date
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extracts a specific claim using a custom resolver function.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extracts all payload claims from token using JJWT 0.12.5 parser.
     *
     * @param token JWT token string
     * @return claims payload
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Checks if token has expired.
     *
     * @param token JWT token string
     * @return true if expired, false otherwise
     */
    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        }
    }

    /**
     * Validates token signature and expiration.
     *
     * @param token JWT token string
     * @return true if token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Validates token against a UserDetails principal.
     *
     * @param token       JWT token string
     * @param userDetails user details
     * @return true if valid and matches user, false otherwise
     */
    public boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    /**
     * Returns the configured expiration duration in milliseconds.
     */
    public long getExpirationTime() {
        return jwtExpirationInMs;
    }
}
