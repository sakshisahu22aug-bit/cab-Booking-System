package com.labmentix.cabbooking.config;

import com.labmentix.cabbooking.entity.Driver;
import com.labmentix.cabbooking.entity.User;
import com.labmentix.cabbooking.enums.UserRole;
import com.labmentix.cabbooking.enums.VehicleType;
import com.labmentix.cabbooking.repository.DriverRepository;
import com.labmentix.cabbooking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds default administrator, test rider, and demo driver upon application startup.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedAdminUser();
        seedDemoRider();
        seedDemoDriver();
    }

    private void seedAdminUser() {
        if (!userRepository.existsByEmail("admin@labmentix.com")) {
            User admin = User.builder()
                    .fullName("System Administrator")
                    .email("admin@labmentix.com")
                    .password(passwordEncoder.encode("Password123!"))
                    .phoneNumber("+919876543210")
                    .role(UserRole.ADMIN)
                    .emailVerified(true)
                    .build();
            userRepository.save(admin);
            log.info("Default administrator account seeded: admin@labmentix.com");
        }
    }

    private void seedDemoRider() {
        if (!userRepository.existsByEmail("rider@labmentix.com")) {
            User rider = User.builder()
                    .fullName("John Rider")
                    .email("rider@labmentix.com")
                    .password(passwordEncoder.encode("Password123!"))
                    .phoneNumber("+919876543211")
                    .role(UserRole.RIDER)
                    .emailVerified(true)
                    .build();
            userRepository.save(rider);
            log.info("Demo rider account seeded: rider@labmentix.com");
        }
    }

    private void seedDemoDriver() {
        if (!userRepository.existsByEmail("driver@labmentix.com")) {
            User driverUser = User.builder()
                    .fullName("Alex Driver")
                    .email("driver@labmentix.com")
                    .password(passwordEncoder.encode("Password123!"))
                    .phoneNumber("+919876543212")
                    .role(UserRole.DRIVER)
                    .emailVerified(true)
                    .build();
            User savedDriverUser = userRepository.save(driverUser);

            Driver driver = Driver.builder()
                    .user(savedDriverUser)
                    .licenseNumber("DL-IND-2024-8899")
                    .vehicleType(VehicleType.SEDAN)
                    .vehicleNumber("DL01AB1234")
                    .vehicleModel("Honda City 2023")
                    .currentLatitude(28.6139) // New Delhi center coordinates
                    .currentLongitude(77.2090)
                    .available(true)
                    .verified(true)
                    .averageRating(4.9)
                    .totalRides(142)
                    .build();
            driverRepository.save(driver);
            log.info("Demo driver account seeded: driver@labmentix.com (DL01AB1234)");
        }
    }
}
