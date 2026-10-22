package com.labmentix.cabbooking.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.labmentix.cabbooking.entity.User;
import com.labmentix.cabbooking.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Custom UserDetails implementation wrapping User entity with Spring Security authentication details.
 */
@Getter
@AllArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final UUID id;
    private final String fullName;
    private final String email;
    @JsonIgnore
    private final String password;
    private final String phoneNumber;
    private final UserRole role;
    private final String profileImageUrl;
    private final boolean emailVerified;
    private final Collection<? extends GrantedAuthority> authorities;
    private final User user;

    public static CustomUserDetails build(User user) {
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );

        return new CustomUserDetails(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPassword(),
                user.getPhoneNumber(),
                user.getRole(),
                user.getProfileImageUrl(),
                user.isEmailVerified(),
                authorities,
                user
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomUserDetails that = (CustomUserDetails) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
