package com.labmentix.cabbooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main Entry Point for Cab Booking System (Uber Clone).
 * Enterprise production-ready Spring Boot application.
 */
@SpringBootApplication
@EnableAsync
public class CabBookingApplication {

    public static void main(String[] args) {
        SpringApplication.run(CabBookingApplication.class, args);
    }
}
