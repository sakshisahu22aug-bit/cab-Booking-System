package com.labmentix.cabbooking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Service handling asynchronous transactional email delivery for booking confirmations and OTPs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Async
    public void sendRideBookingEmail(String toEmail, String riderName, String pickupAddress, String otp) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(toEmail);
            message.setSubject("Ride Confirmed - Cab Booking System");
            message.setText("Hello " + riderName + ",\n\n" +
                    "Your cab has been requested successfully!\n" +
                    "Pickup Location: " + pickupAddress + "\n" +
                    "Your Ride Start OTP: " + otp + "\n\n" +
                    "Share this OTP with your driver once they arrive to begin your journey.\n\n" +
                    "Safe travels,\n" +
                    "Cab Booking System Team");

            mailSender.send(message);
            log.info("Ride confirmation email dispatched to {}", toEmail);
        } catch (Exception e) {
            log.warn("Could not dispatch email to {}: {}", toEmail, e.getMessage());
        }
    }
}
